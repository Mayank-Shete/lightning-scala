package org.viz.lightning

import org.viz.lightning.types.{Three, Plots}

import scala.language.dynamics
import scala.reflect.runtime.universe._

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import scalaj.http._

class Lightning (var host: String) extends Dynamic {

  var session: Int = -1
  var auth: Option[(String, String)] = None

  def this() = this("http://localhost:3000")

  def createSession(sessionName: Option[String] = None) {

    val url = host + "/sessions/"

    implicit val formats = DefaultFormats

    val payload = sessionName.isEmpty match {
      case false => Serialization.write( Map("name" -> sessionName.get) )
      case true => "{}"
    }

    val id = post(url, payload)

    session = id

  }

  def plot(name: String, data: Map[String, Any]): Visualization = {

    this.checkSession()

    val url = host + "/sessions/" + session + "/visualizations/"

    val id = postData(url, data, name)

    new Visualization(this, id.toInt, name)

  }

  def useSession(id: Int): this.type = {
    this.session = id
    this
  }

  def useHost(host: String): this.type = {
    this.host = host
    this
  }

  def checkSession() {
    if (session == -1) {
      this.createSession()
    }
  }

  def post(url: String, payload: String, method: String = "POST"): Int = {

    val request = Http(url).postData(payload).method(method)
      .header("content-type", "application/json")
      .header("accept", "text/plain")

    if (auth.nonEmpty) {
      request.auth(auth.get._1, auth.get._2)
    }

    implicit val formats = DefaultFormats

    val response = request.asString
    val json = parse(response.body)
    (json \ "id").extract[Int]

  }

  def postData(url: String, data: Map[String, Any], name: String, method: String = "POST"): Int = {

    implicit val formats = DefaultFormats

    val blob = Map("data" -> data, "type" -> name)
    val payload = Serialization.write(blob)

    post(url, payload, method)

  }

  def types = Plots.lookup ++ Three.lookup

  def applyDynamic[T: TypeTag](name: String)(args: T): Visualization = {

    val output = types(name)[T](args)
    plot(name, output)

  }

}
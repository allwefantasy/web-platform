package tech.mlsql.serviceframework.platform.form

import tech.mlsql.common.utils.serder.json.JSONTool

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}


object FormParams {

  def reflectParams[T: ru.TypeTag : ClassTag](obj: T) = {
    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val tpe = ru.typeOf[T]
    val instance = mirror.reflect(obj).instance

    val names = tpe.members.filter(f => f.isTerm && !f.isMethod).map(f => f.asTerm)
    names.map { name =>
      val fieldMirror = mirror.reflect(instance).reflectField(name)
      fieldMirror.get
    }
  }

  def toForm[T: ru.TypeTag : ClassTag](obj: T) = {
    val fields = FormParams.reflectParams(obj)
    fields.map { f =>
      f match {
        case a: Select => a.copy(values = a.valueProvider.map(_.apply()).getOrElse(List()))
        case a: Text => a.copy(value = a.valueProvider.map(_.apply()).getOrElse(""))
        case a: Input => a.copy(value = a.valueProvider.map(_.apply()).getOrElse(""))
        case a: CheckBox => a.copy(values = a.valueProvider.map(_.apply()).getOrElse(List()))
        case a: Radio => a.copy(values = a.valueProvider.map(_.apply()).getOrElse(List()))
        case a: Switch => a.copy(values = a.valueProvider.map(_.apply()).getOrElse(List()))
        case a: Slider => a.copy(values = a.valueProvider.map(_.apply()).getOrElse(List()))
        case a: Rate => a.copy(values = a.valueProvider.map(_.apply()).getOrElse(List()))
        case a: InputNumber => a.copy(values = a.valueProvider.map(_.apply()).getOrElse(List()))
        case a: TreeSelect => a.copy(json = JSONTool.toJsonStr(a.valueProvider.map(_.apply()).getOrElse("{}")))
        case a: Transfer => a.copy(
          sourceValues = a.sourceValueProvider.map(_.apply()).getOrElse(List()),
          targetValues = a.targetValueProvider.map(_.apply()).getOrElse(List())
        )
        case a: TimePicker => a.copy(values = a.valueProvider.map(_.apply()).getOrElse(List()))
        case a: Upload => a
        case a: Dynamic => a
        case a: Editor => a.copy(values = a.valueProvider.map(_.apply()).getOrElse(List()))
      }
    }
  }
}

case class KV(name: Option[String], value: Option[String])

case class CheckBox(name: String, values: List[KV], tpe: String = "CheckBox", @transient valueProvider: Option[() => List[KV]])

case class Input(name: String, value: String, tpe: String = "Input", @transient valueProvider: Option[() => String] = None)

case class Text(name: String, value: String, tpe: String = "Text", @transient valueProvider: Option[() => String] = None)

case class Select(name: String, values: List[KV], tpe: String = "Select", @transient valueProvider: Option[() => List[KV]] = None)

case class Radio(name: String, values: List[KV], tpe: String = "Radio", @transient valueProvider: Option[() => List[KV]] = None)

case class Switch(name: String, values: List[KV], tpe: String = "Switch", @transient valueProvider: Option[() => List[KV]] = None)

case class Slider(name: String, values: List[KV], tpe: String = "Slider", @transient valueProvider: Option[() => List[KV]] = None)

case class Rate(name: String, values: List[KV], tpe: String = "Rate", @transient valueProvider: Option[() => List[KV]] = None)

case class InputNumber(name: String, values: List[KV], tpe: String = "InputNumber", @transient valueProvider: Option[() => List[KV]] = None)

case class TreeSelect(name: String, json: String, tpe: String = "TreeSelect", @transient valueProvider: Option[() => List[KV]] = None)

case class Transfer(name: String, sourceValues: List[KV], targetValues: List[KV], tpe: String = "Transfer",
                    @transient sourceValueProvider: Option[() => List[KV]] = None,
                    @transient targetValueProvider: Option[() => List[KV]] = None)

case class TimePicker(name: String, values: List[KV], tpe: String = "TimePicker", @transient valueProvider: Option[() => List[KV]] = None)

case class Upload(name: String, valueProviderName: String, tpe: String = "Upload")

case class Dynamic(name: String, tpe: String = "Dynamic", subTpe: String, depends: List[String], valueProviderName: String)

case class Editor(name: String, tpe: String = "Editor", values: List[KV], @transient valueProvider: Option[() => List[KV]] = None)


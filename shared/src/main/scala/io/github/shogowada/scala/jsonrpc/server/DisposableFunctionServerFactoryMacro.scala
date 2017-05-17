package io.github.shogowada.scala.jsonrpc.server

import io.github.shogowada.scala.jsonrpc.utils.JSONRPCMacroUtils

import scala.reflect.macros.blackbox

class DisposableFunctionServerFactoryMacro[Context <: blackbox.Context](val c: Context) {

  import c.universe._

  lazy val macroUtils = JSONRPCMacroUtils[c.type](c)
  lazy val requestJSONHandlerFactoryMacro = new JSONRPCRequestJSONHandlerFactoryMacro[c.type](c)

  def getOrCreate(
      client: Tree,
      server: Tree,
      disposableFunction: TermName,
      disposableFunctionType: Type
  ): c.Expr[String] = {
    val requestJSONHandlerRepository = macroUtils.getRequestJSONHandlerRepository(server)
    val disposableFunctionMethodNameRepository = macroUtils.getDisposableFunctionMethodNameRepository(client)

    val disposeFunctionMethodHandler = requestJSONHandlerFactoryMacro.createDisposeFunctionMethodHandler(server, client)

    val handler = requestJSONHandlerFactoryMacro.createFromDisposableFunction(client, server, disposableFunction, disposableFunctionType)

    c.Expr[String](
      q"""
          $requestJSONHandlerRepository.addIfAbsent(Constants.DisposeMethodName, () => ($disposeFunctionMethodHandler))

          val methodName: String = $disposableFunctionMethodNameRepository.getOrAddAndNotify(
            $disposableFunction,
            (newMethodName) => { $requestJSONHandlerRepository.add(newMethodName, $handler) }
          )
          methodName
          """
    )
  }
}
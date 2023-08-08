package org.finos.morphir.runtime.quick

import org.finos.morphir.naming.*
import org.finos.morphir.ir.Type.UType
import org.finos.morphir.ir.Value.Value
import org.finos.morphir.ir.Value as V
import org.finos.morphir.datamodel.Data
import org.finos.morphir.ir.distribution.Distribution
import org.finos.morphir.ir.distribution.Distribution.Library
import org.finos.morphir.ir.Type.Type
import org.finos.morphir.runtime.*
import org.finos.morphir.runtime.exports.*
import org.finos.morphir.runtime.services.sdk.MorphirSdk
import org.finos.morphir.runtime.Utils.*
import org.finos.morphir.ir.conversion.*
import org.finos.morphir.datamodel.Util.*
import org.finos.morphir.datamodel.*

import scala.util.{Failure, Success, Try}
import org.finos.morphir.runtime.{EvaluationError, MorphirRuntimeError}
import org.finos.morphir.runtime.environment.MorphirEnv

private[runtime] case class QuickMorphirRuntime(library: Library, store: Store[scala.Unit, UType])
    extends TypedMorphirRuntime {
  // private val store: Store[scala.Unit, UType] = Store.empty //

  def evaluate(entryPoint: FQName, params: Value[scala.Unit, UType]): RTAction[MorphirEnv, MorphirRuntimeError, Data] =
    for {
      tpe <- fetchType(entryPoint)
      res <- evaluate(Value.Reference.Typed(tpe, entryPoint), params)
    } yield res

  def evaluate(value: Value[scala.Unit, UType]): RTAction[MorphirEnv, EvaluationError, Data] =
    EvaluatorQuick.evalAction(value, store, library)

  def fetchType(ref: FQName): RTAction[MorphirEnv, MorphirRuntimeError, UType] = {
    val (pkg, mod, loc) = (ref.getPackagePath, ref.getModulePath, ref.localName)
    val maybeSpec       = library.lookupValueSpecification(PackageName(pkg), ModuleName(mod), loc)
    maybeSpec match {
      case Some(spec) => RTAction.succeed(specificationToType(spec))
      case None       => RTAction.fail(new SpecificationNotFound(s"Could not find $ref during initial type building"))
    }
  }

}

object QuickMorphirRuntime {

  def fromDistribution(distribution: Distribution): QuickMorphirRuntime = distribution match {
    case library: Library =>
      val store = Store.fromDistribution(library)
      QuickMorphirRuntime(library, store)
  }

  def fromDistributionRTAction(distribution: Distribution)
      : RTAction[MorphirEnv, MorphirRuntimeError, QuickMorphirRuntime] =
    RTAction.succeed(fromDistribution(distribution))

}

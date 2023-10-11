package org.cacophony

import com.jme3.math.{FastMath, Quaternion, Vector3f}
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.slf4j.{Logger, LoggerFactory}

class RotationTest extends AnyFunSuite with Matchers with BeforeAndAfter {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private val ROTATION_AXIS = new Vector3f(0, 1, 0)
  private val ROLL_180 = new Quaternion()
  ROLL_180.fromAngleAxis(FastMath.PI, ROTATION_AXIS)

  test("test timing") {
    logger.debug("[RotationTest.test timing] enter.")
    val testObject = new Rotation(1000L, Side.a1, Side.b1) // rotation doesn't matter here

    var result = testObject.slerp(300L)
    logger.debug("[RotationTest.test timing] result: {}", result)
    result = testObject.slerp(350L)
    logger.debug("[RotationTest.test timing] result: {}", result)
    result = testObject.slerp(325L)
    logger.debug("[RotationTest.test timing] result: {}", result)
    result = testObject.slerp(325L)
    logger.debug("[RotationTest.test timing] result: {}", result)
  }
}

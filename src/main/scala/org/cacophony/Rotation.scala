/*
 * $Id$
 *
 * Copyright (c) 2011-2013 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.cacophony

import com.jme3.math.Quaternion
import com.simsilica.es.EntityComponent
import org.slf4j.{Logger, LoggerFactory}

/**
 * Cards only have two sides (face up and face down), but by specifying them this way, when we do the slerp
 * calculations, the rotation is always in the same direction. It's a stylistic choice.
 * 
 * @author Ace McCloud
 */
//noinspection ScalaWeakerAccess
enum Side(val q: Quaternion):
  
  def next(): Side = {
    var ord = this.ordinal + 1
    if ord > 3 then
      ord = 0

    Side.fromOrdinal(ord)
  }
  case faceUp1 extends Side(new Quaternion(0,0,0,1))
  case faceDown1 extends Side(new Quaternion(0,1,0,0))
  case faceUp2 extends Side(new Quaternion(0,0,0,-1))
  case faceDown2 extends Side(new Quaternion(0,-1,0,0))
end Side

/**
 * ECS Component telling the system that the owning entity needs to be rotated
 * @param time the time frame, in seconds, over which the rotation occurs
 * @param startSide Rotate from
 * @param finalSide Rotate to
 *
 * @author Ace McCloud
 */
class Rotation(time: java.lang.Long, startSide: Side, finalSide: Side) extends EntityComponent:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private var cumulativeTime: java.lang.Long = 0L

  private val startRotation: Quaternion = startSide.q
  private val finalRotation: Quaternion = finalSide.q

  def isDone: Boolean = cumulativeTime >= time
  def getFinal: Side = finalSide

  def next(): Rotation =
    val nextSide: Side = finalSide.next()
    new Rotation(time, finalSide, nextSide)

  override def toString: String = s"Rotation($cumulativeTime, $startRotation, $finalRotation)"

  def slerp(tpf: java.lang.Long): Quaternion =
    if cumulativeTime >= time then
      finalRotation
    else
      cumulativeTime += tpf
      if cumulativeTime > time then
        cumulativeTime = time

      val result = Quaternion()
      val slerpTime = (cumulativeTime.toDouble / time.toDouble).toFloat
      logger.trace("[Rotation.slerp] slerpTime: {}", slerpTime)
      result.slerp(startRotation, finalRotation, slerpTime)
      result
end Rotation


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

import com.jme3.`export`.xml.XMLImporter
import com.jme3.`export`.{JmeExporter, JmeImporter, Savable}
import org.cacophony.GameLevels.getClass
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable.Map
import scala.language.postfixOps


/**
 * This loads the GameLevels data from the XML configuration file, rather than
 * hard coding them.
 * 
 * @author Ace McCloud
 */
object GameLevels:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def initialize[T <: GameLevels[_]](fileName: String): Option[T] =
    val resource = getClass.getResource(fileName)
    logger.debug("[GameLevels$.initialize] file is: {}", resource)
    val stream = resource.openStream()
    val importer = new XMLImporter()

    importer.load(stream) match
      case l: T => Some(l)
      case _ => None
end GameLevels

/**
 * A readable/writeable container the holds the GameLevel objects for a specific game.
 * @tparam T The specific type of the GameLevel objects for the game
 * 
 * @author Ace McCloud
 */
class GameLevels[T <: GameLevel] extends Savable:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private var levels: Array[Savable] = _
  private var byOrdinal: Map[Int, T] = _

  override def toString: String = s"GameLevels($byOrdinal)"

  def getStart: T = byOrdinal(1)

  /**
   * Implementation of the write for Savable
   * @param ex exporter providing the capsule we will be writing to
   */
  override def write(ex: JmeExporter): Unit =
    val capsule = ex.getCapsule(this)
    capsule.write(levels, "levels", new Array[Savable](0))

  /**
   * Implementation of the read for Savable
   * @param im importer providing the capsule we will be reading from
   */
  override def read(im: JmeImporter): Unit =
    logger.debug("[GameLevels.read] reading.")
    val capsule = im.getCapsule(this)
    levels = capsule.readSavableArray("levels", new Array[Savable](0))
    logger.debug("[GameLevels.read] got levels: {}", levels)
    setupByOrdinal()

  /**
   * Part of the initialization process after we read the levels, sets up
   * the ordinal map and the "next" references
   */
  private def setupByOrdinal(): Unit =
    var index = 1
    var previous: Option[T] = None

    byOrdinal = levels.map(l => {
      val level = l.asInstanceOf[T]
      level.ordinal = index
      if previous.nonEmpty then
        previous.get.next = level
      previous = Some(level)
      index += 1
      level.ordinal -> level
    }).toMap
end GameLevels

/**
 * Abstract base class for the level objects for the various games
 */
abstract class GameLevel extends Savable:
  var ordinal: Int = _
  var next: GameLevel = _

  protected var label: String = _

  def getLabel: String = label

  def getNext[T <: GameLevel]: Option[T] = next match
    case null => None
    case n: T => Some(n)
end GameLevel

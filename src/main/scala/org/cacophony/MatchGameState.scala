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
import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.font.{BitmapFont, BitmapText}
import com.jme3.math.{ColorRGBA, Vector3f}
import com.jme3.scene.Node
import com.simsilica.es.{EntityData, EntityId}
import com.simsilica.lemur.Label
import com.simsilica.lemur.style.ElementId
import org.cacophony.MatchDeck.getClass
import org.slf4j.{Logger, LoggerFactory}

import java.lang
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
 * A state that the MatchGameState object can be in. Updates and clicks may be handled
 * differently in different states.
 */
trait MGState {
  def update(tpf: Float): MGState
  def click(cardID: java.lang.Long): Unit
}

/**
 * Class that implements the animal card matching/memory game.
 */
class MatchGameState extends BaseAppState {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private var app: AnimalCards = _

  private var ed: EntityData = _

  private var currentState: MGState = _
  private var hud: MatchGameHUD = _
  private var gameLevel: MatchLevel = _
  private var locations: TableauLocations = _

  override def initialize(app: Application): Unit = {
    logger.trace("[MatchGameState.initialize] enter")
    this.app = app.asInstanceOf[AnimalCards]

    MatchDeck.initialize()

    val matchLevels = MatchLevels.initialize()
    gameLevel = matchLevels.getStart

    ed = getState(classOf[EntityDataState]).getEntityData
    hud = new MatchGameHUD(this.app)
  }

  override def cleanup(app: Application): Unit = {
    logger.debug("[MatchGameState.cleanup] enter.")
  }

  override def onEnable(): Unit = {
    logger.debug("[MatchGameState.onEnable] enter.")
    currentState = new MGStart()
  }

  override def onDisable(): Unit = {
    logger.debug("[MatchGameState.onDisable] enter.")
    hud.disable()
  }

  override def update(tpf: Float): Unit = {
    currentState = currentState.update(tpf)
  }

  private val ONE_SECOND = 1000000000L

  /**
   * Defer all clicking to the current state object
   * @param cardID the entity ID of a card that was clicked on
   */
  def cardClicked(cardID: java.lang.Long): Unit = {
    currentState.click(cardID)
  }

  /**
   * Initial state for each game level, sets up the deck and tableau, then waits
   * a half-second before moving to MGDeal state
   */
  private class MGStart() extends MGState {
    private var needsInitialization = true
    private var clock = 0.5f

    override def update(tpf: Float): MGState = {
      var result:MGState = this

      if needsInitialization then
        logger.debug("[MGStart.update] initializing.")
        hud.updateLevel(gameLevel.label)
        hud.updateMatch(gameLevel.getMatchText)
        hud.updateMessage(s"Level: ${gameLevel.label} Matching: ${gameLevel.getMatchText}")
        locations = new TableauLocations(gameLevel)
        logger.debug("[MGStart.update] starting at: {}", TableauLocations.deckLocation)
        MatchDeck.createDeck(gameLevel, TableauLocations.deckLocation, ed)
        needsInitialization = false
      else
        clock -= tpf
        if clock <= 0 then
          result = new MGDeal()

      result
    }

    override def click(cardID: lang.Long): Unit = {
      // ignore clicks in this state
    }
  }

  /**
   * Here we are going to deal the cards into the tableau. When everything has been dealt,
   * move on to MGPlay state.
   */
  private class MGDeal() extends MGState {
    private var needsInitialization = true
    private var dealList: List[EntityId] = Nil
    private var elapsed: java.lang.Float = 0
    private var index: Int = 0

    /**
     * Deal cards one at a time, every half second.
     * @param tpf Time per frame
     * @return this until deal is complete
     */
    override def update(tpf: Float): MGState = {
      var result: MGState = this

      if needsInitialization then
        initialize()
      else if dealList.isEmpty then
        hud.updateMessage("")
        result = new MGPlay()
      else
        elapsed += tpf
        if elapsed > 0.5 then
          elapsed = elapsed - 0.5f
          dealOneCard()

      result
    }

    override def click(cardID: lang.Long): Unit = {
      // ignore clicks in this state
    }

    /**
     * Sets up the list of cards to be dealt.
     */
    private def initialize(): Unit = {
      logger.debug("[MGDeal.initialize] enter.")
      val entities = ed.getEntities(classOf[Position])
      val builder = new ListBuffer[EntityId]()
      entities.forEach(e => builder.append(e.getId))
      dealList = builder.toList
      needsInitialization = false
    }

    private def dealOneCard(): Unit = {
      logger.debug("[MGDeal.dealOneCard] enter.")
      val next = dealList.head
      dealList = dealList.tail

      val nextEntity = ed.getEntity(next, classOf[Position])
      val position = nextEntity.get(classOf[Position])
      ed.setComponent(next, new Move(1.0f, position.getLocation, locations.getLocation(index)))
      index += 1
    }
  }

  /**
   * Here is where the actual game logic is located. Handles card clicks, checks for matches, etc.
   */
  private class MGPlay() extends MGState {
    private var needsInitialization = true
    private var turnCount: Int = 0
    private var chooseCount: Int = _
    private var turned: Array[EntityId] = _
    private var matchesRemaining: Int = gameLevel.matches
    private val discards: mutable.HashSet[EntityId] = new mutable.HashSet[EntityId]()

    override def update(tpf: Float): MGState = {
      var result: MGState = this

      if needsInitialization then
        chooseCount = 0
        turned = new Array[EntityId](gameLevel.cardsPerMatch)
        needsInitialization = false
      else if cardsAreTurning() then
        logger.trace("[MGPlay.update] wait.")
      else if chooseCount == gameLevel.cardsPerMatch then
        logger.trace("[MGPlay.update] check for match.")
        turnCount += 1
        if testForMatch() then
          handleMatch()
        else
          handleNonMatch()
      else if matchesRemaining == 0 then
        hud.updateMessage("Level Complete!")
        result = new MGLevelComplete()

      result
    }

    override def click(cardID: lang.Long): Unit = {
      val entityId = new EntityId(cardID)

      if discards.contains(entityId) then
        logger.trace("[MGPlay.click] ignore click on discarded card.")
      else if turned.contains(entityId) then
        logger.debug("[MGPlay.click] ignore click on selected card.")
      else if chooseCount < gameLevel.cardsPerMatch then
        turned(chooseCount) = entityId
        chooseCount += 1
        turnCard(entityId)
        logger.trace("[MGPlay.click] cards turned: {}", chooseCount)
      // otherwise ignore it
    }

    private def turnCard(entityID: EntityId): Unit = {
      logger.trace("[MGPlay.click] got click on: {}", entityID)
      val entity = ed.getEntity(entityID, classOf[Position])
      logger.trace("[MGPlay.click] entity: {}", entity)

      val side = entity.get(classOf[Position]).getSide

      ed.setComponents(entityID, new Rotation(ONE_SECOND, side, side.next()))

      logger.trace("[MGPlay.click] entity: {}", entity)

      val entities = ed.getEntities(classOf[Rotation])
      entities.forEach(e => logger.trace("[MGPlay.click] next: {}", e))
    }

    private def testForMatch(): Boolean = {
      var result: Boolean = true
      val entities = ed.getEntities(classOf[ModelType])

      val testValue = entities.getEntity(turned(0)).get(classOf[ModelType]).getLabel
      turned.foreach(t => {
        val entity = entities.getEntity(t)
        val label = entity.get(classOf[ModelType]).getLabel
        if label != testValue then
          result = false
      })

      entities.release()
      result
    }

    private def cardsAreTurning(): Boolean = {
      val entities = ed.getEntities(classOf[Rotation])
      val result = entities.size() > 0
      logger.trace("[MGPlay.cardsAreTurning] cards turning: {}", entities.size())
      entities.release()

      result
    }

    private def handleMatch(): Unit = {
      logger.trace("[MatchGameState.handleMatch] enter.")
      hud.adjustScore(gameLevel.cardsPerMatch)
      for (index <- turned.indices) {
        discardCard(turned(index))
        turned(index) = null
      }

      chooseCount = 0
      matchesRemaining -= 1
    }

    private def handleNonMatch(): Unit = {
      logger.trace("[MatchGameState.handleNonMatch] enter.")
      hud.adjustScore(-1)
      for (index <- turned.indices) {
        turnCard(turned(index))
        turned(index) = null
      }

      chooseCount = 0
    }

    private def discardCard(cardID: EntityId): Unit = {
      logger.trace("[MGDeal.dealOneCard] enter.")

      val nextEntity = ed.getEntity(cardID, classOf[Position])
      val position = nextEntity.get(classOf[Position])
      ed.setComponent(cardID, new Move(1.0f, position.getLocation, TableauLocations.discardLocation))
      discards.add(cardID)
    }
  }

  /**
   * After all the cards have been matched, we end up here. From here, we either go
   * to the next level or we go to MGGameOver
   */
  private class MGLevelComplete extends MGState {
    override def update(tpf: Float): MGState = {
      cleanup()
      gameLevel = gameLevel.getNext

      if gameLevel == null then
        hud.updateMessage("Game Over")
        new MGGameOver()
      else new MGStart()
    }

    override def click(cardID: lang.Long): Unit = {}

    private def cleanup(): Unit = {
      val entities = ed.getEntities(classOf[ModelType])
      entities.forEach(e => ed.removeEntity(e.getId))
    }
  }

  /**
   * Game is over, return to menu.
   */
  private class MGGameOver extends MGState {
    private var time = 2.5f
    override def update(tpf: Float): MGState = {
      time -= tpf

      if time > 0 then
        logger.debug("[MGGameOver.update] still waiting.")
      else
        logger.debug("[MGGameOver.update] return to menu.")
        getState(classOf[MainMenuState]).setEnabled(true)
        getStateManager.detach(MatchGameState.this)

      this
    }

    /**
     * Ignore clicks in this state.
     * @param cardID Why is the player even clicking?
     */
    override def click(cardID: lang.Long): Unit = {}
  }
}

/**
 * Utility object that creates a "deck of cards" - a bunch of entities with appropriate
 * components
 */
object MatchDeck {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private var cardNames: Array[String] = _

  /**
   * Read the list of available animal graphics files from a new-line delimited
   * card file.
   */
  def initialize(): Unit = {
    val stream = getClass.getResource("/cardList.txt").openStream()
    val raw = stream.readAllBytes()
    stream.close()
    cardNames = new String(raw).split("\n")

    logger.trace("[MatchDeck.initialize] card names: {}", cardNames)
  }

  def createDeck(gameLevel: MatchLevel, startLocation: Vector3f, ed: EntityData): Unit = {
    logger.trace("[MatchDeck.createDeck] deck starts at: {}", startLocation)
    val cardLabels = createCardLabels(gameLevel)
    logger.trace("[MatchDeck.createDeck] got: {}", cardLabels)
    var nextLocation = startLocation

    cardLabels.foreach(l => {
      createCard(ed, nextLocation, l)
      nextLocation = new Vector3f(nextLocation.x, nextLocation.y, nextLocation.z - 0.02f)
    })
  }

  /**
   * Creates a randomized array with the card/animal names that match one
   * of the available graphics files
   * @param gameLevel Specifies number of matches and cards per match
   * @return List of string names, randomized
   */
  private def createCardLabels(gameLevel: MatchLevel): List[String] = {
    val result = new ListBuffer[String]()
    for (outerIndex <- 0 until gameLevel.matches) {
      for (_ <- 0 until gameLevel.cardsPerMatch) {
        result.append(cardNames(outerIndex))
      }
    }
    Random.shuffle(result.toList)
  }

  /**
   * Creates entity for one card
   * @param ed EntityData that we will be adding entities to
   * @param location Computed display location from the tableau definition
   * @param name Name of the animal that maps to a given graphic file
   */
  private def createCard(ed: EntityData, location: Vector3f, name: String): Unit = {
    val cardID = ed.createEntity
    ed.setComponents(
      cardID,
      new Position(location, Side.b1.q, Side.b1),
      new ModelType(ToyModelFactory.MATCH_CARD, name)
    )

    logger.trace("[MatchGameState.initialize] cardID is: {}", cardID)
  }
}

/**
 * This loads the MatchLevels data from the XML configuration file, rather than
 * hard coding them.
 */
object MatchLevels {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def initialize(): MatchLevels = {
    val resource = getClass.getResource("/matchLevels.xml")
    logger.debug("[MatchLevel.initialize] file is: {}", resource)
    val stream = resource.openStream()
    val importer = new XMLImporter()

    importer.load(stream) match {
      case l: MatchLevels => l
      case _ => null
    }
  }
}

/**
 * The defined levels for the animal match/memory game.
 */
class MatchLevels extends Savable {
  private var levels: Array[Savable] = _
  private var byOrdinal: Map[Int, MatchLevel] = _

  def getStart: MatchLevel = byOrdinal(1)

  override def write(ex: JmeExporter): Unit = {
    val capsule = ex.getCapsule(this)
    capsule.write(levels, "levels", new Array[Savable](0))
  }

  override def read(im: JmeImporter): Unit = {
    val capsule = im.getCapsule(this)
    levels = capsule.readSavableArray("levels", new Array[Savable](0))

    setupByOrdinal()
  }

  private def setupByOrdinal(): Unit = {
    var index = 1
    var previous: MatchLevel = null

    byOrdinal = levels.map(l => {
      val level = l.asInstanceOf[MatchLevel]
      level.ordinal = index
      if previous != null then
        previous.next = level
      previous = level
      index += 1
      level.ordinal -> level
    }).toMap
  }
}

/**
 * Definition for a single level of the match/memory game.
 * Mostly just a data blob with label, number of matches, and cards per match
 */
class MatchLevel extends Savable:
  var ordinal: Int = _
  var label: String = _
  var matches: Int = _
  var cardsPerMatch: Int = _
  var next: MatchLevel = _

  def getNext: MatchLevel = next

  def getMatchText: String = cardsPerMatch match {
    case 2 => "PAIRS"
    case 3 => "TRIPLES"
    case 4 => "QUADRUPLES"
  }

  override def write(ex: JmeExporter): Unit = {
    val capsule = ex.getCapsule(this)
    capsule.write(label, "label", "")
    capsule.write(matches, "matches", 0)
    capsule.write(cardsPerMatch, "cardsPerMatch", 0)
  }

  override def read(im: JmeImporter): Unit = {
    val capsule = im.getCapsule(this)
    label = capsule.readString("label", "")
    matches = capsule.readInt("matches", 0)
    cardsPerMatch = capsule.readInt("cardsPerMatch", 0)
  }
end MatchLevel

object TableauLocations {
  def deckLocation = new Vector3f(-7, 3, 0.0)
  def discardLocation = new Vector3f(7, 3, 0.0)

  private val X_SPACING = 1.2f
  private val Y_SPACING = 1.75f
}

/**
 * Define the locations for a game of match, which fall into three categories:
 * 1. The deck - before the game, the shuffled deck of cards
 * 2. The layout - where the cards are dealt
 * 3. The discard - matched pairs go here
 * @param gameLevel Game level defines number of matches and cards per match.
 */
class TableauLocations(gameLevel: MatchLevel) {
  private val cards = gameLevel.cardsPerMatch * gameLevel.matches
  private val rows = Math.floor(Math.sqrt(cards)).toInt
  private val columns = (cards + rows - 1) / rows
  private val startX = -(columns / 2) * TableauLocations.X_SPACING
  private val startY = (rows / 2) * TableauLocations.Y_SPACING

  private val layout: Array[Vector3f] = calculateLayout()

  def getLocation(index: Int): Vector3f = layout(index)

  private def calculateLayout(): Array[Vector3f] = {
    val result = new Array[Vector3f](cards)
    var rowIndex = 0
    var colIndex = 0
    var nextX = startX
    var nextY = startY
    for (index <- 0 until cards) {
      result(index) = new Vector3f(nextX, nextY, 0)

      colIndex += 1
      nextX += TableauLocations.X_SPACING
      if colIndex >= columns then
        colIndex = 0
        nextX = startX
        rowIndex += 1
        nextY -= TableauLocations.Y_SPACING
    }

    result
  }
}

/**
 * Displays score and status info to the user.
 * @param app The application, so we can get the guiNode and guiFont
 */
class MatchGameHUD(app: AnimalCards) {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private var guiNode: Node = _

  private var scoreText: BitmapText = _
  private var levelText: BitmapText = _
  private var matchText: BitmapText = _
  private var messageText: Label = _
  private var score: Int = 0

  createHUD()

  def disable(): Unit = {
    guiNode.detachChild(scoreText)
    guiNode.detachChild(levelText)
    guiNode.detachChild(matchText)
    guiNode.detachChild(messageText)
  }

  def adjustScore(by: Int): Unit = {
    score += by
    scoreText.setText(s"Score: $score")
  }

  def updateLevel(levelName: String): Unit = {
    levelText.setText(s"Level: $levelName")
  }

  def updateMatch(matchString: String): Unit = {
    matchText.setText(s"Match: $matchString")
  }

  def updateMessage(message: String): Unit = {
    messageText.setText(message)
    val size = messageText.getPreferredSize
    logger.warn("[MatchGameHUD.updateMessage] size: {}", size)
    val newX = 900f - (size.x / 2)
    logger.warn("[MatchGameHUD.updateMessage] newX: {}", newX)
    messageText.setLocalTranslation(newX, 450f, 0f)
  }

  private def createHUD(): Unit = {
    guiNode = this.app.getGuiNode
    val guiFont = this.app.getGuiFont

    addScore(guiNode, guiFont)
    addLevel(guiNode, guiFont)
    addMatchSize(guiNode, guiFont)
    addMessage(guiNode)
  }

  private def addScore(guiNode: Node, guiFont: BitmapFont): Unit = {
    scoreText = createText(guiNode, guiFont, 50f)
    scoreText.setText("Score: 0") // the text
  }

  private def addLevel(guiNode: Node, guiFont: BitmapFont): Unit = {
    levelText = createText(guiNode, guiFont, 200f)
    levelText.setText("Level: ONE") // the text
  }

  private def addMatchSize(guiNode: Node, guiFont: BitmapFont): Unit = {
    matchText = createText(guiNode, guiFont, 350f)
    matchText.setText("Match: PAIRS") // the text
  }

  private def addMessage(guiNode: Node): Unit = {
    messageText = new Label("", new ElementId(ToyStyles.TITLE_ID), "retro")
    messageText.setLocalTranslation(900f, 450f, 0f)
    guiNode.attachChild(messageText)
  }

  private def createText(guiNode: Node, guiFont: BitmapFont, xLocation: Float): BitmapText = {
    val result = new BitmapText(guiFont)
    result.setSize(guiFont.getCharSet.getRenderedSize.toFloat) // font size
    result.setColor(ColorRGBA.Green) // font color
    result.setLocalTranslation(xLocation, result.getLineHeight, 0) // position
    guiNode.attachChild(result)

    result
  }
}
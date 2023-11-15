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

import com.jme3.`export`.{JmeExporter, JmeImporter}
import com.jme3.app.Application
import com.jme3.font.{BitmapFont, BitmapText}
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import com.simsilica.es.{EntityData, EntityId}
import org.cacophony.MatchDeck.getClass
import org.slf4j.{Logger, LoggerFactory}

import java.lang
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
 * Class that implements the animal card matching/memory game.
 *
 * @author Ace McCloud
 */
class MatchGameState extends AnimalGameState:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private var app: AnimalCards = _

  private var ed: EntityData = _

  private var hud: MatchGameHUD = new MatchGameHUD()
  private var gameLevel: MatchLevel = _
  private var locations: MatchTableauLocations = _

  override def initialize(app: Application): Unit =
    logger.debug("[MatchGameState.initialize] enter")
    this.app = app.asInstanceOf[AnimalCards]
    app.getStateManager.attach(hud)

    val matchLevels = GameLevels.initialize[GameLevels[MatchLevel]]("/matchLevels.xml").orNull
    gameLevel = matchLevels.getStart

    ed = getState(classOf[EntityDataState]).getEntityData

  override def cleanup(app: Application): Unit =
    logger.debug("[MatchGameState.cleanup] enter.")

  override def onEnable(): Unit =
    logger.debug("[MatchGameState.onEnable] enter.")
    currentState = new MGStart()

  override def onDisable(): Unit =
    logger.debug("[MatchGameState.onDisable] enter.")
    app.getStateManager.detach(hud)

  override def update(tpf: Float): Unit =
    if hud.isReady then
      currentState = currentState.update(tpf)

  /**
   * Initial state for each game level, sets up the deck and tableau, then waits
   * a half-second before moving to MGDeal state
   */
  private class MGStart() extends AnimalCardState:
    private var needsInitialization = true
    private var clock = 0.5f

    override def update(tpf: Float): AnimalCardState =
      var result:AnimalCardState = this

      if needsInitialization then
        logger.debug("[MGStart.update] initializing.")
        hud.updateLevel(gameLevel.getLabel)
        hud.updateMatch(gameLevel.getMatchText)
        hud.updateMessage(s"Level: ${gameLevel.getLabel} Matching: ${gameLevel.getMatchText}")
        locations = new MatchTableauLocations(gameLevel)
        logger.debug("[MGStart.update] starting at: {}", MatchTableauLocations.deckLocation)
        MatchDeck.createDeck(gameLevel, MatchTableauLocations.deckLocation, ed)
        needsInitialization = false
      else
        clock -= tpf
        if clock <= 0 then
          result = new MGDeal()

      result
  end MGStart

  /**
   * Here we are going to deal the cards into the tableau. When everything has been dealt,
   * move on to MGPlay state.
   */
  private class MGDeal() extends AnimalCardState:
    private var needsInitialization = true
    private var dealList: List[EntityId] = Nil
    private var elapsed: java.lang.Float = 0
    private var index: Int = 0

    /**
     * Deal cards one at a time, every half second.
     * @param tpf Time per frame
     * @return this until deal is complete
     */
    override def update(tpf: Float): AnimalCardState =
      var result: AnimalCardState = this

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

    /**
     * Sets up the list of cards to be dealt.
     */
    private def initialize(): Unit =
      logger.debug("[MGDeal.initialize] enter.")
      val entities = ed.getEntities(classOf[Position])
      val builder = new ListBuffer[EntityId]()
      entities.forEach(e => builder.append(e.getId))
      dealList = builder.toList
      needsInitialization = false

    private def dealOneCard(): Unit =
      logger.debug("[MGDeal.dealOneCard] enter.")
      val next = dealList.head
      dealList = dealList.tail

      val nextEntity = ed.getEntity(next, classOf[Position])
      val position = nextEntity.get(classOf[Position])
      ed.setComponent(next, new Move(1.0f, position.getLocation, locations.getLocation(index)))
      index += 1
  end MGDeal

  /**
   * Here is where the actual game logic is located. Handles card clicks, checks for matches, etc.
   */
  private class MGPlay() extends AnimalCardState:
    private var needsInitialization = true
    private var turnCount: Int = 0
    private var chooseCount: Int = _
    private var turned: Array[EntityId] = _
    private var matchesRemaining: Int = gameLevel.matches
    private val discards: mutable.HashSet[EntityId] = new mutable.HashSet[EntityId]()

    override def update(tpf: Float): AnimalCardState =
      var result: AnimalCardState = this

      if needsInitialization then
        clicked = ed.getEntities(classOf[Clicked])
        chooseCount = 0
        turned = new Array[EntityId](gameLevel.cardsPerMatch)
        needsInitialization = false
      else
        checkForClick()

      if cardsAreTurning() then
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
        clicked.release()
        result = new MGLevelComplete()

      result

    override protected def click(entityId: EntityId): Unit =
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

      ed.removeComponent(entityId, classOf[Clicked])

    private def turnCard(entityID: EntityId): Unit =
      logger.trace("[MGPlay.click] got click on: {}", entityID)
      val entity = ed.getEntity(entityID, classOf[Position])
      logger.trace("[MGPlay.click] entity: {}", entity)

      val side = entity.get(classOf[Position]).getSide

      ed.setComponents(entityID, new Rotation(Constants.ONE_SECOND, side, side.next()))

      logger.trace("[MGPlay.click] entity: {}", entity)

      val entities = ed.getEntities(classOf[Rotation])
      entities.forEach(e => logger.trace("[MGPlay.click] next: {}", e))

    private def testForMatch(): Boolean =
      var result: Boolean = true
      val entities = ed.getEntities(classOf[ModelType])

      val testValue = entities.getEntity(turned(0)).get(classOf[ModelType]).getLabel
      turned.foreach(t =>
        val entity = entities.getEntity(t)
        val label = entity.get(classOf[ModelType]).getLabel
        if label != testValue then result = false
      )

      entities.release()
      result

    private def cardsAreTurning(): Boolean =
      val entities = ed.getEntities(classOf[Rotation])
      val result = entities.size() > 0
      logger.trace("[MGPlay.cardsAreTurning] cards turning: {}", entities.size())
      entities.release()

      result

    private def handleMatch(): Unit =
      logger.trace("[MatchGameState.handleMatch] enter.")
      hud.adjustScore(gameLevel.cardsPerMatch)
      for (index <- turned.indices)
        discardCard(turned(index))
        turned(index) = null

      chooseCount = 0
      matchesRemaining -= 1

    private def handleNonMatch(): Unit =
      logger.trace("[MatchGameState.handleNonMatch] enter.")
      hud.adjustScore(-1)
      for (index <- turned.indices)
        turnCard(turned(index))
        turned(index) = null

      chooseCount = 0

    private def discardCard(cardID: EntityId): Unit =
      logger.trace("[MGDeal.dealOneCard] enter.")

      val nextEntity = ed.getEntity(cardID, classOf[Position])
      val position = nextEntity.get(classOf[Position])
      ed.setComponent(cardID, new Move(1.0f, position.getLocation, MatchTableauLocations.discardLocation))
      discards.add(cardID)
  end MGPlay

  /**
   * After all the cards have been matched, we end up here. From here, we either go
   * to the next level or we go to MGGameOver
   */
  private class MGLevelComplete extends AnimalCardState:
    override def update(tpf: Float): AnimalCardState =
      cleanup()
      gameLevel = gameLevel.getNext[MatchLevel].orNull

      if gameLevel == null then
        hud.updateMessage("Game Over")
        new MGGameOver()
      else new MGStart()

    private def cleanup(): Unit =
      val entities = ed.getEntities(classOf[ModelType])
      entities.forEach(e => ed.removeEntity(e.getId))
  end MGLevelComplete

  /**
   * Game is over, return to menu.
   */
  private class MGGameOver extends AnimalCardState:
    private var time = 2.5f
    override def update(tpf: Float): AnimalCardState =
      time -= tpf

      if time > 0 then
        logger.debug("[MGGameOver.update] still waiting.")
      else
        logger.debug("[MGGameOver.update] return to menu.")
        getState(classOf[MainMenuState]).setEnabled(true)
        getStateManager.detach(MatchGameState.this)

      this
  end MGGameOver
end MatchGameState

/**
 * Utility object that creates a "deck of cards" - a bunch of entities with appropriate
 * components
 */
object MatchDeck:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def createDeck(gameLevel: MatchLevel, startLocation: Vector3f, ed: EntityData): Unit =
    logger.trace("[MatchDeck.createDeck] deck starts at: {}", startLocation)
    val cardLabels = createCardLabels(gameLevel)
    logger.trace("[MatchDeck.createDeck] got: {}", cardLabels)
    var nextLocation = startLocation

    cardLabels.foreach(l =>
      createCard(ed, nextLocation, l)
      nextLocation = new Vector3f(nextLocation.x, nextLocation.y, nextLocation.z - 0.02f)
    )

  /**
   * Creates a randomized array with the card/animal names that match one
   * of the available graphics files
   * @param gameLevel Specifies number of matches and cards per match
   * @return List of string names, randomized
   */
  private def createCardLabels(gameLevel: MatchLevel): List[String] =
    val result = new ListBuffer[String]()
    for (outerIndex <- 0 until gameLevel.matches)
      for (_ <- 0 until gameLevel.cardsPerMatch)
        result.append(AnimalNames.animalNames(outerIndex))
    Random.shuffle(result.toList)

  /**
   * Creates entity for one card
   * @param ed EntityData that we will be adding entities to
   * @param location Computed display location from the tableau definition
   * @param name Name of the animal that maps to a given graphic file
   */
  private def createCard(ed: EntityData, location: Vector3f, name: String): Unit =
    val cardID = ed.createEntity
    ed.setComponents(
      cardID,
      new Position(location, Side.faceDown1.q, Side.faceDown1),
      new ModelType(CardModelFactory.MATCH_CARD, name)
    )

    logger.trace("[MatchGameState.initialize] cardID is: {}", cardID)
end MatchDeck


/**
 * Definition for a single level of the match/memory game.
 * Mostly just a data blob with label, number of matches, and cards per match
 * 
 * @author Ace McCloud
 */
class MatchLevel extends GameLevel:
  var matches: Int = _
  var cardsPerMatch: Int = _

  def getMatchText: String = cardsPerMatch match
    case 2 => "PAIRS"
    case 3 => "TRIPLES"
    case 4 => "QUADRUPLES"

  override def write(ex: JmeExporter): Unit =
    val capsule = ex.getCapsule(this)
    capsule.write(label, "label", "")
    capsule.write(matches, "matches", 0)
    capsule.write(cardsPerMatch, "cardsPerMatch", 0)

  override def read(im: JmeImporter): Unit =
    val capsule = im.getCapsule(this)
    label = capsule.readString("label", "")
    matches = capsule.readInt("matches", 0)
    cardsPerMatch = capsule.readInt("cardsPerMatch", 0)
end MatchLevel

/**
 * Some constants used in the location calculations
 * 
 * @author Ace McCloud
 */
object MatchTableauLocations:
  def deckLocation = new Vector3f(-7, 3, 0.0)
  def discardLocation = new Vector3f(7, 3, 0.0)

  private val X_SPACING = 1.2f
  private val Y_SPACING = 1.75f
end MatchTableauLocations

/**
 * Define the locations for a game of match, which fall into three categories:
 * 1. The deck - before the game, the shuffled deck of cards
 * 2. The layout - where the cards are dealt
 * 3. The discard - matched pairs go here
 * @param gameLevel Game level defines number of matches and cards per match.
 * 
 * @author Ace McCloud
 */
class MatchTableauLocations(gameLevel: MatchLevel):
  private val cards = gameLevel.cardsPerMatch * gameLevel.matches
  private val rows = Math.floor(Math.sqrt(cards)).toInt
  private val columns = (cards + rows - 1) / rows
  private val startX = -(columns / 2) * MatchTableauLocations.X_SPACING
  private val startY = (rows / 2) * MatchTableauLocations.Y_SPACING

  private val layout: Array[Vector3f] = calculateLayout()

  def getLocation(index: Int): Vector3f = layout(index)

  private def calculateLayout(): Array[Vector3f] =
    val result = new Array[Vector3f](cards)
    var rowIndex = 0
    var colIndex = 0
    var nextX = startX
    var nextY = startY
    for (index <- 0 until cards)
      result(index) = new Vector3f(nextX, nextY, 0)

      colIndex += 1
      nextX += MatchTableauLocations.X_SPACING
      if colIndex >= columns then
        colIndex = 0
        nextX = startX
        rowIndex += 1
        nextY -= MatchTableauLocations.Y_SPACING

    result
end MatchTableauLocations

/**
 * Displays score and status info to the user.
 * 
 * @author Ace McCloud
 */
class MatchGameHUD extends GameHud:
  private var matchText: BitmapText = _

  override def onEnable(): Unit =
    super.onEnable()
    guiNode.attachChild(matchText)

  override def onDisable(): Unit =
    super.onDisable()
    guiNode.detachChild(matchText)

  def updateMatch(matchString: String): Unit =
    matchText.setText(s"Match: $matchString")

  override protected def createHUD(): Unit =
    super.createHUD()

    val guiFont = app.getGuiFont

    addMatchSize(guiNode, guiFont)

  private def addMatchSize(guiNode: Node, guiFont: BitmapFont): Unit =
    matchText = createText(guiNode, guiFont, 350f)
    matchText.setText("Match: PAIRS") // the text
end MatchGameHUD

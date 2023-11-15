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
import com.jme3.app.state.BaseAppState
import com.jme3.math
import com.jme3.math.Vector3f
import com.simsilica.es.{Entity, EntityData, EntityId}
import org.cacophony.SequenceTableauLocations.{SEPARATE_X_SPACING, STACKED_X_SPACING, STACKED_Y_SPACING, STACKED_Z_SPACING}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
 * Implements a game in which a sequence of cards is displayed and the user must
 * remember the sequence to score.
 *
 * @author Ace McCloud
 */
class SequenceGameState extends BaseAppState:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private var app: AnimalCards = _

  private var ed: EntityData = _

  private val hud: SequenceHud = new SequenceHud()
  private var playState: AnimalCardState = _
  private var gameLevel: SequenceLevel = _
  private var locations: SequenceTableauLocations = _

  override def initialize(app: Application): Unit =
    logger.debug("[SequenceGameState.initialize] enter.")
    this.app = app.asInstanceOf[AnimalCards]
    app.getStateManager.attach(hud)

    val matchLevels = GameLevels.initialize[GameLevels[SequenceLevel]]("/sequenceLevels.xml").orNull
    gameLevel = matchLevels.getStart

    ed = getState(classOf[EntityDataState]).getEntityData

  override def cleanup(app: Application): Unit = {}

  override def onEnable(): Unit =
    logger.debug("[SequenceGameState.onEnable] enter.")
    playState = new SGStart()

  override def onDisable(): Unit =
    app.getStateManager.detach(hud)

  override def update(tpf: Float): Unit =
    if hud.isReady then
      playState = playState.update(tpf)

  /**
   * The start state for the sequence game, and also for each new or retried level.
   */
  private class SGStart extends AnimalCardState:
    /**
     * Do some initialization, then do a WaitState before going to SGDeal to show
     * the player the card sequence.
     * @param tpf Time per frame
     *  @return Next or current state
     */
    override def update(tpf: Float): AnimalCardState =
      logger.trace("[SGStart.update] enter.")

      gameLevel.start()
//      hud.updateMessage(s"Watch sequence, length: ${gameLevel.getCurrentLength}")
      hud.updateMessage(s"Begin level ${gameLevel.getLabel}")
      hud.updateLevel(gameLevel.getLabel)

      locations = new SequenceTableauLocations(gameLevel)
      logger.debug("[SGStart.update] starting at: {}", MatchTableauLocations.deckLocation)
      new WaitState(0.5f, new SGDeal)
  end SGStart

  /**
   * In this game, the deal consists of displaying the card sequence that the player
   * will then have to remember and recreate.
   */
  //noinspection DuplicatedCode
  private class SGDeal extends AnimalCardState:
    private var needsInitialization = true
    private var dealList: List[EntityId] = Nil
    private var dealCard: Option[EntityId] = None
    private var index: Int = 0

    private var pausing: Boolean = false
    private var pause: Float = 0f

    /**
     * Here we do the dealing. Mostly we're flipping back and forth between dealing a card,
     * pausing to let the player see it, and getting ready to deal the next card.
     * Until we've dealt all the cards, then we transition to SGPlay
     * @param tpf Time per frame
     *  @return Next or current state
     */
    override def update(tpf: Float): AnimalCardState =
      var result: AnimalCardState = this

      if needsInitialization then
        initialize()
      else if pause > 0 then
        pause -= tpf
        // do nothing
      else if pausing then
        // pause has reached zero, go ahead and remove the card and continue
        pausing = false
        ed.removeEntity(dealCard.orNull)
        logger.debug("[SGDeal.update] removing: {}", dealCard)
        dealCard = None
      else if dealComplete() then
        // display the card for an extra half second
        pausing = true
        pause = 0.5f
      else if dealCard.isEmpty && dealList.isEmpty then
        result = new SGPlay()
      else if dealCard.isEmpty then
        dealOneCard()

      result

    /**
     * Sets up the list of cards to be dealt.
     */
    private def initialize(): Unit =
      logger.debug("[SGDeal.initialize] enter.")
      hud.updateMessage("")
      dealList = SequenceDeck.createDeck(gameLevel, SequenceTableauLocations.deckLocation, ed)
      logger.debug("[SGDeal.initialize] deal list: {}", dealList)
      needsInitialization = false

    /**
     * Sets up one card to be dealt. It moves from the pack to the center, turns over, then
     * there is a pause before we remove it and deal the next card.
     */
    private def dealOneCard(): Unit =
      logger.debug("[SGDeal.dealOneCard] enter.")
      val card = dealList.head
      dealList = dealList.tail

      val nextCard = ed.getEntity(card, classOf[Position])
      val position = nextCard.get(classOf[Position])
      ed.setComponent(card, new Move(0.25f, position.getLocation, SequenceTableauLocations.spotLocation))
      ed.setComponent(card, new Rotation(Constants.ONE_SECOND / 2, position.getSide, position.getSide.next()))
      index += 1
      dealCard = Some(card)

    /**
     * Determines if the card being dealt has reached its final state - center of board and flipped.
     * @return true if the move and the rotate have both completed
     */
    private def dealComplete(): Boolean =
      var result = false

      dealCard.foreach(c =>
        logger.trace("[SGDeal.dealComplete] dealCard is: {}", c)
        val nextCard = ed.getEntity(c, classOf[Move], classOf[Rotation])
        logger.trace("[SGDeal.dealComplete] got: {}", nextCard)
        val components = nextCard.getComponents
        result = components.map(_ == null).forall(identity)
        logger.trace("[SGDeal.dealComplete] result: {}", result)
      )

      result
  end SGDeal

  /**
   * Menu options to be displayed if the user makes a mistake recreating the sequence.
   */
  private val failMenus = Array[MenuOption](
    new ACSMenuOption("Let's try that again", new SGRetryLevel()),
    new ACSMenuOption("I'm done", new SGGameOver())
  )

  /**
   * Menu options to be displayed if the user completes a level.
   */
  private val successMenu = Array[MenuOption](
    new ACSMenuOption("Let's do that again!", new SGRetryLevel()),
    new ACSMenuOption("Level up, please!", new SGNextLevel()),
    new ACSMenuOption("I'm done", new SGGameOver())
  )

  /**
   * In this state, the player is trying to recreate the sequence displayed in the deal.
   * They do this by clicking on the cards at the bottom. If the card is correct, it will
   * be duplicated and the duplicate added to the displayed sequence, until they hit
   * the last card. If they make an error, throw up the failMenu. If they complete
   * the level, throw up the successMenu.
   */
  private class SGPlay extends AnimalCardState:
    private var needsInitialization = true
    private var index = 0
    private var nextState: AnimalCardState = this

    /**
     * This state, the program sets up and then just waits for clicks.
     * @param tpf Time per frame
     *  @return Next or current state
     */
    override def update(tpf: Float): AnimalCardState =
      logger.trace("[SGPlay.update] enter.")

      if needsInitialization then
        initialize()
      else
        checkForClick()

      nextState

    /**
     * When a card from the layout is clicked, duplicate it and move it to the display
     * @param entityId The ID of a card that was clicked (not necessarily a legal click)
     */
    override protected def click(entityId: EntityId): Unit =
      logger.debug("[SGPlay.click] clicked: {}", entityId)

      val card = ed.getEntity(entityId, classOf[ModelType])
      if card.get(classOf[ModelType]).getType == CardModelFactory.SEQUENCE_LAYOUT_CARD then
        logger.debug("[SGPlay.click] layout card: {}", card)
        createCard(entityId)

      // ignore anything else

      // clean up the clicked component
      ed.removeComponent(entityId, classOf[Clicked])

    /**
     * Put up the layout on the bottom, one card of each image used on this level.
     * These are the clickable cards players will use to recreate the displayed sequence.
     */
    private def initialize(): Unit =
      logger.debug("[SGPlay.initialize] enter.")
      SequenceDeck.createLayout(gameLevel, locations, ed)
      clicked = ed.getEntities(classOf[Clicked])

      needsInitialization = false

    /**
     * When the user clicks on a card, duplicate it and move the duplicate into its spot
     * in the "showcase"
     * @param entityId ID of the card that was clicked on/to be duplicated
     */
    private def createCard(entityId: EntityId): Unit =
      val card = ed.getEntity(entityId, classOf[ModelType], classOf[Position])
      val position = card.get(classOf[Position])
      val modelType = card.get(classOf[ModelType])
      if modelType.getLabel != gameLevel.getCardLabels(index) then
        fail()
      else
        // a1 is face up
        val newID = SequenceDeck.createCard(
          ed,
          position.getLocation,
          CardModelFactory.SEQUENCE_CARD,
          modelType.getLabel,
          Side.faceUp1)
        val toLocation = locations.getShowcase(index).clone()
        val move = new Move(1.0f, position.getLocation.clone(), toLocation)
        ed.setComponent(newID, move)
        index += 1
        logger.debug("[SGPlay.createCard] index is: {}", index)
        if index >= gameLevel.getCurrentLength then succeed()

    /**
     * User clicked on an incorrect card.
     */
    private def fail(): Unit =
      logger.debug("[SGPlay.fail] enter.")
      hud.adjustScore(gameLevel.getFailScore)
      cardCleanup()
      nextState = new SGPrompt("Incorrect", failMenus)

    /**
     * User completed the current sequence correctly. Either move on to
     * the next step of the level or to the successMenu.
     */
    private def succeed(): Unit =
      logger.debug("[SGPlay.succeed] enter")
      hud.adjustScore(gameLevel.getSuccessScore)
      cardCleanup()

      if gameLevel.isComplete then
        logger.debug("[SGPlay.succeed] level complete.")
        nextState = new SGPrompt("Level Complete", successMenu)
      else
        hud.updateMessage("Success!")
        nextState = new SGContinueLevel()

    /**
     * Deletes all of the cards at the end of play.
     */
    private def cardCleanup(): Unit =
      val cards = ed.getEntities(classOf[ModelType])
      cards.forEach(c => {
        logger.debug("[SGPlay.cleanup] cleanup: {}", c)
        ed.removeEntity(c.getId)
      })
      cards.release()
  end SGPlay

  /**
   * Player has completed the current sequence and is moving to the next
   * step of the same level.
   */
  private class SGContinueLevel extends AnimalCardState:
    override def update(tpf: Float): AnimalCardState =
      gameLevel.nextStep()
      new WaitState(0.5f, new SGDeal())
  end SGContinueLevel

  /**
   * Put up a menu and ask the user what they want to do next.
   * @param prompt The title at the top of the menu
   * @param menuOptions The list of MenuOption values
   */
  private class SGPrompt(prompt: String, menuOptions: Array[MenuOption]) extends AnimalCardState:
    private var needsInitialization = true
    private var menu: SimpleMenu = _

    override def update(tpf: Float): AnimalCardState =
      var result: AnimalCardState = this

      if needsInitialization then
        menu = new SimpleMenu(app, prompt, menuOptions)
        needsInitialization = false
      else if menu.isChoiceMade then
        val choice = menu.getChoice.asInstanceOf[ACSMenuOption]
        result = choice.getNext
        menu = null

      result
  end SGPrompt

  /**
   * When the user makes a mistake OR if they complete the level, they can retry the level
   * with a new sequence.
   */
  private class SGRetryLevel extends AnimalCardState:
    override def update(tpf: Float): AnimalCardState =
      gameLevel.start()
      logger.debug("[SGRetryLevel.update] enter.")
      new WaitState(0.5f, new SGDeal())
  end SGRetryLevel

  /**
   * If the user completes one level successfully, they have the option of moving up
   * to the next level.
   */
  private class SGNextLevel extends AnimalCardState:
    override def update(tpf: Float): AnimalCardState =
      logger.debug("[SGNextLevel.update] enter.")
      gameLevel.getNext[SequenceLevel] match
        case None =>
          new SGGameOver()
        case Some(n) =>
          gameLevel = n
          new WaitState(0.5f, new SGStart())
  end SGNextLevel

  /**
   * The user has completed all the levels or has chosen not to continue
   * to the next level or replay the current level.
   */
  private class SGGameOver extends AnimalCardState:
    private var time = 2.5f
    override def update(tpf: Float): AnimalCardState =
      time -= tpf

      if time > 0 then
        logger.debug("[SGGameOver.update] still waiting.")
      else
        logger.debug("[SGGameOver.update] return to menu.")
        getState(classOf[MainMenuState]).setEnabled(true)
        getStateManager.detach(SequenceGameState.this)
      this
  end SGGameOver
end SequenceGameState

/**
 * Class representing a level in the sequence game, spelling out the game parameters
 * for that level.
 *
 * @author Ace McCloud
 */
class SequenceLevel extends GameLevel:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private var startLength: Int = _
  private var endLength: Int = _
  private var incrementSize: Int = _
  private var cardChoices: Int = _

  private var cardNames: List[String] = _
  private var currentLength: Int = _

  override def toString: String = s"SequenceLevel($label: $startLength/$endLength/$cardChoices)"

  /**
   * When the player starts a level, initialize things, including the full sequence for this
   * play.
   */
  def start(): Unit =
    currentLength = startLength
    createCardLabels()

  /**
   * Creates a randomized array with the card/animal names that match one
   * of the available graphics files
   */
  private def createCardLabels(): Unit =
    val result = new ListBuffer[String]()
    for (_ <- 0 until endLength) do
      val index = Random.between(0, cardChoices)
      val next = AnimalNames.animalNames(index)
      result.addOne(next)

    cardNames = result.toList
    logger.info("[SequenceLevel.createCardLabels] generated: {}", cardNames)

  /**
   * Player has completed the current step, move to the next step of this level
   */
  def nextStep(): Unit =
    if currentLength < endLength then
      currentLength += incrementSize

    logger.debug("[SequenceLevel.nextStep] currentLength now: {}", currentLength)

  def getSuccessScore: Int = ((currentLength - startLength + 1) * cardChoices) / 3
  def getFailScore: Int = - (currentLength - startLength + 1) / 2 // negative because it is a penalty.
  def isComplete: Boolean = currentLength >= endLength

  /**
   * Returns the list of labels from the current sequence for the current step.
   * @return A list of string card names
   */
  def getCardLabels: List[String] =
    logger.debug("[SequenceLevel.getCardLabels] length: {} cards: {}", currentLength, cardNames)
    cardNames.slice(0, currentLength)
  def getCardChoices: Int = cardChoices
  def getCurrentLength: Int = currentLength
  def getLength: Int = endLength

  /**
   * Save method defined for game objects that can be saved to/read from files.
   * @param ex Exporter object that creates the capsule we will be writing to
   */
  override def write(ex: JmeExporter): Unit =
    val capsule = ex.getCapsule(this)
    capsule.write(label, "label", "")
    capsule.write(startLength, "startLength", 0)
    capsule.write(endLength, "endLength", 0)
    capsule.write(incrementSize, "incrementSize", 0)
    capsule.write(cardChoices, "cardChoices", 0)

  /**
   * Read method defined for game objects that can be saved to/read from files.
   * @param im Importer object that creates the capsule we will be reading from
   */
  override def read(im: JmeImporter): Unit =
    val capsule = im.getCapsule(this)
    label = capsule.readString("label", "")
    startLength = capsule.readInt("startLength", 0)
    endLength = capsule.readInt("endLength", 0)
    incrementSize = capsule.readInt("incrementSize", 0)
    cardChoices = capsule.readInt("cardChoices", 0)
end SequenceLevel

/**
 * Constants relating to where cards can be placed on the screen.
 *
 * @author Ace McCloud
 */
object SequenceTableauLocations:
  def deckLocation = new Vector3f(-7, 3, 0.0)
  def spotLocation = new Vector3f(0f, 0f, 0f)

  private val STACKED_X_SPACING = 0.6f
  private val STACKED_Y_SPACING = 0.4f
  private val STACKED_Z_SPACING = 0.05f

  private val SEPARATE_X_SPACING = 1.2f

/**
 * Define the locations for the sequence game, which fall into three categories:
 * 1. The deck - before the game, the shuffled deck of cards
 * 2. The spot - during the Reveal phase, the center of the display, where cards
 *               from the deck are displayed
 * 3. The layout - at the bottom, one of each card type, for the user to click
 *                 on during the Remembrance
 * 4. The showcase - where the user clicked sequence is displayed
 * @param gameLevel Game level defines number of matches and cards per match.
 *
 * @author Ace McCloud
 */
class SequenceTableauLocations(gameLevel: SequenceLevel):
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private val layout: Array[Vector3f] = calculateLayout()
  private val showcase: Array[Vector3f] = calculateShowcase()

  def getLayout(index: Int): Vector3f = layout(index)
  def getShowcase(index: Int): Vector3f = showcase(index)

  /**
   * Calculates, based on the number of layout cards, how to display them correctly,
   * bottom of the screen centered on the Y axis/x = 0.
   * @return
   */
  private def calculateLayout(): Array[Vector3f] =
    val result = new Array[Vector3f](gameLevel.getCardChoices)

    val startX = -(SEPARATE_X_SPACING * gameLevel.getCardChoices) / 2
    for index <- result.indices do
      result(index) = new Vector3f(startX + (SEPARATE_X_SPACING * index), -3f, 0f)
      logger.debug("[SequenceTableauLocations.calculateLayout] next: {}", result(index))

    result

  /**
   * Calculates, based on the number of cards in the sequence, how to display them correctly.
   * Centered on (0,0)
   * @return An array containing the positions for the card sequence
   */
  private def calculateShowcase(): Array[Vector3f] =
    val result = new Array[Vector3f](gameLevel.getLength)

    val startX = -(STACKED_X_SPACING * gameLevel.getLength)/2
    val startY = (STACKED_Y_SPACING * gameLevel.getLength)/2
    val startZ = (STACKED_Z_SPACING * gameLevel.getLength)/2
    for index <- result.indices do
      result(index) = new Vector3f(
        startX + (STACKED_X_SPACING * index),
        startY - (STACKED_Y_SPACING * index),
        startZ + (STACKED_Z_SPACING * index))
      logger.trace("[SequenceTableauLocations.calculateShowcase] next: {}", result(index))

    result
end SequenceTableauLocations

/**
 * Utility routines for creating the various card groups used in the game.
 *
 * @author Ace McCloud
 */
object SequenceDeck:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Creates the cards for the "deck" - the card sequence the player will need to recreate
   * @param gameLevel The game level object supplying the list of card names making up the sequence
   * @param startLocation The location of the deck, which is where the cards start
   * @param ed EntityData we will be adding the created cards to
   * @return The list of EntityId's for the cards when created
   */
  def createDeck(gameLevel: SequenceLevel, startLocation: Vector3f, ed: EntityData): List[EntityId] =
    logger.trace("[SequenceDeck.createDeck] deck starts at: {}", startLocation)
    val cardLabels = gameLevel.getCardLabels
    logger.debug("[SequenceDeck.createDeck] got: {}", cardLabels)
    var nextLocation = startLocation

    cardLabels.map(l =>
      val card = createCard(ed, nextLocation, CardModelFactory.SEQUENCE_CARD, l, Side.faceDown1)
      nextLocation = new Vector3f(nextLocation.x, nextLocation.y, nextLocation.z - 0.02f)

      card)

  /**
   * Creates the "layout" - the cards the player will click on to recreate the target sequence
   * @param gameLevel The game level object, contains the number of card types
   * @param locations Object that calculates the locations for the cards in the layout
   * @param ed The EntityData we will be adding the created cards to
   */
  def createLayout(gameLevel: SequenceLevel, locations: SequenceTableauLocations, ed: EntityData): Unit =
    logger.debug("[SequenceDeck.createLayout] enter.")
    for (index <- 0 until gameLevel.getCardChoices) do
      val label = AnimalNames.animalNames(index)
      val location = locations.getLayout(index)
      createCard(ed, location, CardModelFactory.SEQUENCE_LAYOUT_CARD, label, Side.faceUp1)

  /**
   * Creates entity for one card
   *
   * @param ed       EntityData that we will be adding entities to
   * @param location Computed display location from the tableau definition
   * @param name     Name of the animal that maps to a given graphic file
   */
  def createCard(ed: EntityData, location: Vector3f, cardType: String, name: String, side: Side): EntityId =
    val cardID = ed.createEntity
    ed.setComponents(
      cardID,
      new Position(location, side.q, side),
      new ModelType(cardType, name)
    )

    logger.trace("[SequenceDeck.createCard] cardID is: {}", cardID)
    cardID
end SequenceDeck

class SequenceHud extends GameHud

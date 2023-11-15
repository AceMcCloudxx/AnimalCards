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

import com.simsilica.es.{EntityId, EntitySet}

/**
 * A state that the MatchGameState object can be in. Updates and clicks may be handled
 * differently in different states.
 * 
 * @author Ace McCloud
 */
trait AnimalCardState:
  protected var clicked: EntitySet = _

  /**
   * Called by the enclosing BaseAppState object to handle state based updates.
   * @param tpf Time per frame
   * @return Next or current state
   */
  def update(tpf: Float): AnimalCardState

  /**
   * A utility function used by some states to map click on a card to our function
   * that handles clicks.
   */
  protected def checkForClick(): Unit = {
    clicked.applyChanges()
    clicked.forEach(e => {
      val id = e.getId
      click(id)
    })
  }

  protected def click(entityId: EntityId): Unit = {}
end AnimalCardState

/**
 * A utility state that can be used by the games when transitioning between
 * two states with a time delay.
 * @param time Amount of time to wait
 * @param next The state to transition to when the timer has expired
 *
 * @author Ace McCloud
 */
class WaitState(time: Float, next: AnimalCardState) extends AnimalCardState:
  private var remaining: Float = time
  def update(tpf: Float): AnimalCardState = {
    var result:AnimalCardState  = this
    remaining -= tpf

    if remaining <= 0 then
      result = next

    result
  }
end WaitState

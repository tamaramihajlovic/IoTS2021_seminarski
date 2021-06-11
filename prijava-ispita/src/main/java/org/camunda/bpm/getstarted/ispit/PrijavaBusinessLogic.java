/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.getstarted.ispit;

import org.camunda.bpm.engine.cdi.jsf.TaskForm;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Named
public class PrijavaBusinessLogic {

  // Inject the entity manager
  @PersistenceContext
  private EntityManager entityManager;

  // Inject task form available through the camunda cdi artifact
  @Inject
  private TaskForm taskForm;

  private static Logger LOGGER = Logger.getLogger(PrijavaBusinessLogic.class.getName());

  public void persistPrijava(DelegateExecution delegateExecution) {
    // Create new order instance
    PrijavaEntity prijavaEntity = new PrijavaEntity();

    // Get all process variables
    Map<String, Object> variables = delegateExecution.getVariables();

    // Set order attributes
    prijavaEntity.setStudent((String) variables.get("student"));
    prijavaEntity.setIndeks((String) variables.get("indeks"));
    prijavaEntity.setIspit((String) variables.get("ispit"));

    // Persist order instance and flush. After the flush the
    // id of the order instance is set.
    entityManager.persist(prijavaEntity);
    entityManager.flush();

    // Remove no longer needed process variables
    delegateExecution.removeVariables(variables.keySet());

    // Add newly created order id as process variable
    delegateExecution.setVariable("prijavaId", prijavaEntity.getId());
  }

  public PrijavaEntity getPrijava(Long prijavaId) {
    // Load order entity from database
    return entityManager.find(PrijavaEntity.class, prijavaId);
  }

  /*
    Merge updated order entity and complete task form in one transaction. This ensures
    that both changes will rollback if an error occurs during transaction.
   */
  public void mergePrijavaAndCompleteTask(PrijavaEntity prijavaEntity) {
    // Merge detached order entity with current persisted state
    entityManager.merge(prijavaEntity);
    try {
      // Complete user task from
      taskForm.completeTask();
    } catch (IOException e) {
      // Rollback both transactions on error
      throw new RuntimeException("Cannot complete task", e);
    }
  }

  public void rejectPrijava(DelegateExecution delegateExecution) {
	PrijavaEntity prijava = getPrijava((Long) delegateExecution.getVariable("prijavaId"));
    LOGGER.log(Level.INFO, "\n\n\nSending Email:\nPostovani/Postovana {0}, {1} prijava ispita {2} nije izvrsena. Pokusajte ponovo.\n\n\n", new String[]{prijava.getStudent(), prijava.getIndeks(), prijava.getIspit()});
  }

}

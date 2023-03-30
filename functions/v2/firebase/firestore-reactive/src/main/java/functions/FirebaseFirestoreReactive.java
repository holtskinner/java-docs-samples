/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package functions;

// [START functions_cloudevent_firebase_reactive]
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.functions.CloudEventsFunction;
import com.google.events.cloud.firestore.v1.DocumentEventData;
import com.google.events.cloud.firestore.v1.Value;
import com.google.protobuf.InvalidProtocolBufferException;
import io.cloudevents.CloudEvent;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class FirebaseFirestoreReactive implements CloudEventsFunction {
  private static final Logger logger = Logger.getLogger(FirebaseFirestoreReactive.class.getName());
  private final Firestore firestore;

  private static final String FIELD_KEY = "original";

  public FirebaseFirestoreReactive() {
    this(FirestoreOptions.getDefaultInstance().getService());
  }

  public FirebaseFirestoreReactive(Firestore firestore) {
    this.firestore = firestore;
  }

  @Override
  public void accept(CloudEvent event)
      throws InvalidProtocolBufferException, InterruptedException, ExecutionException {
    DocumentEventData firestorEventData = DocumentEventData
        .parseFrom(event.getData().toBytes());

    Map<String, Value> fields = firestorEventData.getValue().getFieldsMap();
    if (!fields.containsKey(FIELD_KEY)) {
      logger.warning("Document does not contain original field");
      return;
    }
    String currValue = fields.get(FIELD_KEY).getStringValue();
    String newValue = currValue.toUpperCase();

    if (currValue.equals(newValue)) {
      logger.info("Value is already upper-case");
      return;
    }

    String affectedDoc = firestorEventData.getValue()
        .getName()
        .split("/documents/")[1]
        .replace("\"", "");

    logger.info(String.format("Replacing values: %s --> %s", currValue, newValue));

    this.firestore
        .document(affectedDoc)
        .set(Map.of(FIELD_KEY, newValue), SetOptions.merge())
        .get();
  }
}

// [END functions_cloudevent_firebase_reactive]

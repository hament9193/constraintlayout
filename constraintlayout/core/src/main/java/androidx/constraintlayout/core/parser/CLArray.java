/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.constraintlayout.core.parser;

public class CLArray extends CLContainer {
  public CLArray(char[] content) {
    super(content);
  }

  public static CLElement allocate(char[] content) {
    return new CLArray(content);
  }

  protected String toJSON() {
    StringBuilder content = new StringBuilder(getDebugName() + "[");
    boolean first = true;
    for (int i = 0; i < mElements.size(); i++) {
      if (!first) {
        content.append(", ");
      } else {
        first = false;
      }
      content.append(mElements.get(i).toJSON());
    }
    return content + "]";
  }
}
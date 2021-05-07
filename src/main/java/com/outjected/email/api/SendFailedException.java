/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.outjected.email.api;

/**
 * Thrown when an email fails to be sent.
 * 
 * @author Cody Lerum
 */
public class SendFailedException extends MailException {

    public SendFailedException() {
        super();
    }

    public SendFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public SendFailedException(String message) {
        super(message);
    }

    public SendFailedException(Throwable cause) {
        super(cause);
    }
}

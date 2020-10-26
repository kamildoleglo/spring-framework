/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.support

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import reactor.test.StepVerifier
import kotlin.coroutines.Continuation

/**
 * @author Sebastien Deleuze
 */
class KotlinAopUtilsTests {

	@Test
	fun `invoke getMessage`() {
		val method = ClassWithCoroutines::class.java.getMethod("getMessage", Continuation::class.java)
		val message = "foo"
		val instanceWithCoroutines = ClassWithCoroutines(message)
		val returnValue = AopUtils.invokeJoinpointUsingReflection(instanceWithCoroutines, method, emptyArray())
		StepVerifier.create(returnValue as Publisher<*>).expectNext(message).verifyComplete()
	}

	@Test
	fun `invoke getMessages`() {
		val method = ClassWithCoroutines::class.java.getMethod("getMessages", Continuation::class.java)
		val message = "foo"
		val instanceWithCoroutines = ClassWithCoroutines(message)
		val returnValue = AopUtils.invokeJoinpointUsingReflection(instanceWithCoroutines, method, emptyArray())
		StepVerifier.create(returnValue as Publisher<*>).expectNext(message).expectNext(message).verifyComplete()
	}

	open class ClassWithCoroutines(private val message: String) {

		suspend fun getMessage(): String {
			delay(10)
			return message
		}

		suspend fun getMessages(): Flow<String> {
			delay(10)
			return flow {
				emit(message)
				delay(10)
				emit(message)
			}
		}
	}

}
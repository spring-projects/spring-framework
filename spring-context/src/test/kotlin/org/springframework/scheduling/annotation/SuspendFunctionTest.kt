package org.springframework.scheduling.annotation

import org.springframework.stereotype.Component

@Component

class SuspendFunctionTest {

	@Scheduled(fixedRate = 200)
	suspend fun test() {
		println("as")
	}
}
package com.jtakumi.watchtechnow

import kotlin.test.Test
import kotlin.test.assertTrue

class ProjectConfigurationTest {
    @Test
    fun projectStartsWithSharedUi() {
        assertTrue("Watch Tech Now".isNotBlank())
    }
}

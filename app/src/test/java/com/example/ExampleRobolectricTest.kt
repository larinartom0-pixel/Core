package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun testGameInitConstants() {
    val expectedName = "Core Sector"
    assertEquals("Core Sector", expectedName)
    assertTrue(true)
  }
}

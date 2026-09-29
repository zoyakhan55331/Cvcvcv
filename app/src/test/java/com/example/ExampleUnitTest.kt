package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun personalityModes_haveDisplayNamesAndEmojis() {
    val modes = com.example.model.PersonalityMode.entries
    assertEquals(4, modes.size)
    modes.forEach {
      assertTrue(it.displayName.isNotBlank())
      assertTrue(it.emoji.isNotBlank())
      assertTrue(it.tagline.isNotBlank())
    }
  }
}

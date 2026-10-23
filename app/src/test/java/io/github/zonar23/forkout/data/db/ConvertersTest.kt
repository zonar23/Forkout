package io.github.zonar23.forkout.data.db

import io.github.zonar23.forkout.data.db.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun muscleGroups_roundTrip() {
        val groups = setOf(MuscleGroup.LEGS, MuscleGroup.CHEST, MuscleGroup.CORE)

        val encoded = converters.muscleGroupsToString(groups)

        assertEquals("CHEST,CORE,LEGS", encoded)
        assertEquals(groups, converters.stringToMuscleGroups(encoded))
    }

    @Test
    fun muscleGroups_emptyAndUnknownValues() {
        assertEquals("", converters.muscleGroupsToString(emptySet()))
        assertEquals(emptySet<MuscleGroup>(), converters.stringToMuscleGroups(""))
        assertEquals(setOf(MuscleGroup.BACK), converters.stringToMuscleGroups("BACK,REMOVED_GROUP"))
    }

    @Test
    fun instant_roundTrip() {
        val instant = Instant.ofEpochMilli(1_761_200_000_000)

        assertEquals(instant, converters.millisToInstant(converters.instantToMillis(instant)))
        assertNull(converters.instantToMillis(null))
        assertNull(converters.millisToInstant(null))
    }
}

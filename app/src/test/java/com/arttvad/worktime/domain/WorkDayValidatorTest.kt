package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calculation.WorkDayValidationError
import com.arttvad.worktime.domain.calculation.WorkDayValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkDayValidatorTest {
    @Test
    fun validWorkDayPasses() {
        assertNull(WorkDayValidator.validate(workedMinutes = 8 * 60, overtimeMinutes = 60))
    }

    @Test
    fun zeroWorkedTimeIsRejected() {
        assertEquals(
            WorkDayValidationError.INVALID_WORKED_TIME,
            WorkDayValidator.validate(workedMinutes = 0, overtimeMinutes = 0),
        )
    }

    @Test
    fun overtimeCannotExceedWorkedTime() {
        assertEquals(
            WorkDayValidationError.OVERTIME_EXCEEDS_WORKED,
            WorkDayValidator.validate(workedMinutes = 60, overtimeMinutes = 61),
        )
    }

    @Test
    fun moreThanTwentyFourHoursIsRejected() {
        assertEquals(
            WorkDayValidationError.INVALID_WORKED_TIME,
            WorkDayValidator.validate(workedMinutes = 24 * 60 + 1, overtimeMinutes = 0),
        )
    }
}

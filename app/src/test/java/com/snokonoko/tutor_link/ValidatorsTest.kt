package com.snokonoko.tutor_link

import com.snokonoko.tutor_link.data.Role
import com.snokonoko.tutor_link.domain.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun shortUsernameGetsTheDemoDomain() {
        assertEquals("dylangorrah3+student1@gmail.com", Validators.normaliseLogin("  Student1 "))
    }

    @Test
    fun fullEmailIsKeptButLowercased() {
        assertEquals("m.pretorius@rosebank.ac.za", Validators.normaliseLogin("M.Pretorius@Rosebank.ac.za"))
    }

    @Test
    fun signInNeedsLoginAndPassword() {
        assertEquals("Enter your email or username", Validators.signInError("", "123456"))
        assertEquals("Enter your password", Validators.signInError("student1", ""))
        assertNull(Validators.signInError("student1", "123456"))
    }

    @Test
    fun campusEmailRule() {
        assertTrue(Validators.isCampusEmail("a.b@rosebank.ac.za"))
        assertTrue(Validators.isCampusEmail("a.b@tutor.rosebank.ac.za"))
        assertFalse(Validators.isCampusEmail("a.b@gmail.com"))
        assertFalse(Validators.isCampusEmail("a.b@fakerosebank.ac.za"))
    }

    @Test
    fun registerReportsEachFieldLikeTheDemo() {
        val e = Validators.register("", "someone@gmail.com", "123", "123")
        assertEquals("Enter your full name", e.name)
        assertEquals("Use your rosebank.ac.za campus address", e.email)
        assertEquals("Password needs at least 6 characters", e.password)
        assertFalse(e.ok)
    }

    @Test
    fun registerCatchesMismatchedPasswords() {
        val e = Validators.register("Dr A", "a@rosebank.ac.za", "123456", "654321")
        assertEquals("The two passwords do not match", e.confirm)
    }

    @Test
    fun registerPassesWithGoodInput() {
        assertTrue(Validators.register("Dr A", "a@rosebank.ac.za", "123456", "123456").ok)
    }

    @Test
    fun invalidEmailIsRejected() {
        assertEquals("Enter a valid email address", Validators.register("A", "not-an-email", "123456", "123456").email)
    }

    @Test
    fun newPasswordRules() {
        assertNotNull(Validators.newPassword("12345", "12345"))
        assertEquals("The two passwords do not match", Validators.newPassword("1234567", "7654321"))
        assertNull(Validators.newPassword("1234567", "1234567"))
    }

    @Test
    fun wrongCardIsCaught() {
        assertEquals(
            "This is a tutor account. Go back and choose Tutor.",
            Validators.roleMismatch(chosen = Role.Admin, actual = Role.Tutor),
        )
        assertNull(Validators.roleMismatch(Role.Lecturer, Role.Lecturer))
    }

    @Test
    fun roleKeysMapToRoles() {
        assertEquals(Role.Tutor, Role.of("tutor"))
        assertEquals(Role.Admin, Role.of("admin"))
        assertNull(Role.of("student"))
    }

    @Test
    fun studentCountRange() {
        assertNotNull(Validators.studentCount("0"))
        assertNotNull(Validators.studentCount("61"))
        assertNotNull(Validators.studentCount("abc"))
        assertNull(Validators.studentCount("12"))
    }

    @Test
    fun rateAndMinimumMarkRanges() {
        assertNotNull(Validators.rate("20"))
        assertNull(Validators.rate("150"))
        assertNotNull(Validators.minMark("40"))
        assertNull(Validators.minMark("75"))
    }

    @Test
    fun messageNeedsTopicAndBody() {
        assertEquals("Add a topic and a message", Validators.message("", "body"))
        assertNotNull(Validators.message("x".repeat(121), "body"))
        assertNull(Validators.message("Week 8", "Arrays please"))
    }
}

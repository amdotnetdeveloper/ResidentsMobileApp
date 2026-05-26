package com.example.data

import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class ResidentRepository(
    private val residentDao: ResidentDao,
    private val userDao: UserDao
) {

    fun getResidents(userId: Long): Flow<List<Resident>> {
        return residentDao.getAllResidents(userId)
    }

    suspend fun findResidentByUniqueComposite(userId: Long, block: String, buildingNumber: String, name: String): Resident? {
        return residentDao.getResidentByUniqueComposite(userId, block, buildingNumber, name)
    }

    suspend fun insertResident(resident: Resident) {
        residentDao.insertResident(resident)
    }

    suspend fun deleteResident(resident: Resident) {
        residentDao.deleteResident(resident)
    }

    // Secure User Registration and Login Operations
    suspend fun getUserByPhone(phoneNumber: String): User? {
        return userDao.getUserByPhone(phoneNumber)
    }

    suspend fun getUserById(userId: Long): User? {
        return userDao.getUserById(userId)
    }

    suspend fun registerUser(phoneNumber: String, passwordRaw: String): Long {
        val hash = hashPassword(passwordRaw)
        val user = User(phoneNumber = phoneNumber.trim(), passwordHash = hash)
        return userDao.insertUser(user)
    }

    suspend fun loginUser(phoneNumber: String, passwordRaw: String): User? {
        val user = userDao.getUserByPhone(phoneNumber.trim()) ?: return null
        val hash = hashPassword(passwordRaw)
        return if (user.passwordHash == hash) user else null
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

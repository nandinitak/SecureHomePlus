package com.example.securehomeplus.data.repository

import com.example.securehomeplus.data.database.dao.FamilyMemberDao
import com.example.securehomeplus.data.database.entities.FamilyMember

class FamilyRepository(private val dao: FamilyMemberDao) {

    suspend fun addFamilyMember(member: FamilyMember): Long {
        return dao.insertFamilyMember(member)
    }

    suspend fun updateFamilyMember(member: FamilyMember) {
        dao.updateFamilyMember(member)
    }

    suspend fun deleteFamilyMember(member: FamilyMember) {
        dao.deleteFamilyMember(member)
    }

    suspend fun getFamilyMembers(userEmail: String): List<FamilyMember> {
        return dao.getFamilyMembersForUser(userEmail)
    }

    suspend fun getFamilyMemberById(id: Int): FamilyMember? {
        return dao.getFamilyMemberById(id)
    }
}

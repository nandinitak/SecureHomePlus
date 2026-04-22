package com.example.securehomeplus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.securehomeplus.data.database.entities.FamilyMember

@Dao
interface FamilyMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMember): Long

    @Update
    suspend fun updateFamilyMember(member: FamilyMember)

    @Delete
    suspend fun deleteFamilyMember(member: FamilyMember)

    @Query("SELECT * FROM family_members WHERE userEmail = :email ORDER BY name ASC")
    suspend fun getFamilyMembersForUser(email: String): List<FamilyMember>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getFamilyMemberById(id: Int): FamilyMember?

    @Query("DELETE FROM family_members WHERE userEmail = :email")
    suspend fun deleteAllForUser(email: String)
}

package com.xentoryx.expensey.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xentoryx.expensey.core.data.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAccounts(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Query("SELECT * FROM accounts WHERE isSynced = 0")
    suspend fun getUnsyncedAccounts(): List<AccountEntity>

    @Query("UPDATE accounts SET isSynced = 1 WHERE accountId = :accountId")
    suspend fun markAccountSynced(accountId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE accountId = :accountId")
    suspend fun getAccountById(accountId: String): AccountEntity?

    @Query("DELETE FROM accounts WHERE accountId = :accountId")
    suspend fun deleteAccountById(accountId: String)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE accountId = :accountId")
    suspend fun adjustBalance(accountId: String, amount: Double)

    @Transaction
    suspend fun replaceAccounts(accounts: List<AccountEntity>) {
        deleteAllAccounts()
        insertAccounts(accounts)
    }
}

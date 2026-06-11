package com.example.minseo5.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SpendingDao {

    @Query("SELECT * FROM spending WHERE usedDate LIKE :monthPrefix || '%' ORDER BY usedDate DESC, id DESC")
    List<SpendingRecord> getByMonth(String monthPrefix);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM spending WHERE usedDate LIKE :monthPrefix || '%'")
    long getMonthTotal(String monthPrefix);

    @Query("SELECT COUNT(*) FROM spending WHERE usedDate = :usedDate AND amount = :amount AND purpose = :purpose")
    int countDuplicate(String usedDate, long amount, String purpose);

    @Query("SELECT * FROM spending ORDER BY usedDate DESC, id DESC")
    List<SpendingRecord> getAll();

    @Insert
    void insert(SpendingRecord record);

    @Update
    void update(SpendingRecord record);

    @Delete
    void delete(SpendingRecord record);
}

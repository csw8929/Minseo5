package com.example.minseo5.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "spending")
public class SpendingRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String usedDate;   // "2026-06-11"
    public String usedTime;   // "14:54" (HH:mm), 문자에 시각이 있을 때만. 직접입력/구버전은 null
    public long amount;       // 원 단위
    public String purpose;    // 용도
    public String entryDate;  // "2026-06-11T21:27:00" ISO 8601
}

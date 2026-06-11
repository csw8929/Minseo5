package com.example.minseo5.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.minseo5.R;
import com.example.minseo5.db.SpendingDatabase;
import com.example.minseo5.db.SpendingDao;
import com.example.minseo5.db.SpendingRecord;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SpendingEntryDialog extends DialogFragment {

    public interface OnSaveListener {
        void onSave();
    }

    private static final String KEY_ID = "id";
    private static final String KEY_DATE = "usedDate";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_PURPOSE = "purpose";
    private static final String KEY_IS_EDIT = "isEdit";

    private EditText etDate, etAmount, etPurpose;
    private boolean isEdit;
    private int recordId;
    private SpendingDao dao;
    private OnSaveListener saveListener;

    public static SpendingEntryDialog newAddInstance(String date, long amount, String purpose) {
        SpendingEntryDialog d = new SpendingEntryDialog();
        Bundle args = new Bundle();
        args.putBoolean(KEY_IS_EDIT, false);
        args.putString(KEY_DATE, date != null ? date : "");
        args.putLong(KEY_AMOUNT, amount);
        args.putString(KEY_PURPOSE, purpose != null ? purpose : "");
        d.setArguments(args);
        return d;
    }

    public static SpendingEntryDialog newEditInstance(SpendingRecord record) {
        SpendingEntryDialog d = new SpendingEntryDialog();
        Bundle args = new Bundle();
        args.putBoolean(KEY_IS_EDIT, true);
        args.putInt(KEY_ID, record.id);
        args.putString(KEY_DATE, record.usedDate != null ? record.usedDate : "");
        args.putLong(KEY_AMOUNT, record.amount);
        args.putString(KEY_PURPOSE, record.purpose != null ? record.purpose : "");
        d.setArguments(args);
        return d;
    }

    public void setOnSaveListener(OnSaveListener listener) {
        this.saveListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_spending_entry, null);

        etDate = v.findViewById(R.id.et_date);
        etAmount = v.findViewById(R.id.et_amount);
        etPurpose = v.findViewById(R.id.et_purpose);
        Button btnDelete = v.findViewById(R.id.btn_delete);

        dao = SpendingDatabase.getInstance(requireContext()).spendingDao();
        Bundle args = requireArguments();
        isEdit = args.getBoolean(KEY_IS_EDIT, false);
        recordId = args.getInt(KEY_ID, -1);

        String date = args.getString(KEY_DATE, "");
        if (date.isEmpty()) {
            date = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        }
        etDate.setText(date);

        long amount = args.getLong(KEY_AMOUNT, 0);
        if (amount > 0) etAmount.setText(String.valueOf(amount));

        etPurpose.setText(args.getString(KEY_PURPOSE, ""));

        if (isEdit) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(btn -> {
                SpendingRecord r = new SpendingRecord();
                r.id = recordId;
                r.usedDate = "";
                r.amount = 0;
                r.purpose = "";
                r.entryDate = "";
                dao.delete(r);
                if (saveListener != null) saveListener.onSave();
                dismiss();
            });
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(isEdit ? "수정" : "지출 추가")
                .setView(v)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dlg = (AlertDialog) getDialog();
        if (dlg == null) return;

        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String date = etDate.getText().toString().trim();
            String amtStr = etAmount.getText().toString().trim();
            String purpose = etPurpose.getText().toString().trim();

            if (date.isEmpty()) {
                etDate.setError("날짜를 입력해주세요");
                return;
            }
            long amount = parseAmount(amtStr);
            if (amount <= 0) {
                etAmount.setError("금액을 입력해주세요");
                return;
            }

            String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA).format(new Date());
            SpendingRecord r = new SpendingRecord();
            r.usedDate = date;
            r.amount = amount;
            r.purpose = purpose;
            r.entryDate = now;

            if (isEdit) {
                r.id = recordId;
                dao.update(r);
            } else {
                dao.insert(r);
            }

            if (saveListener != null) saveListener.onSave();
            dismiss();
        });
    }

    private long parseAmount(String s) {
        try {
            return Long.parseLong(s.replace(",", "").replace("원", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

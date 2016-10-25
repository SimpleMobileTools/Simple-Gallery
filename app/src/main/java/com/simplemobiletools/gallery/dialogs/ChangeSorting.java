package com.simplemobiletools.gallery.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.simplemobiletools.gallery.Config;
import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;

public class ChangeSorting extends AlertDialog.Builder implements DialogInterface.OnClickListener {
    private static Config mConfig;
    private static ChangeDialogListener mListener;
    private static View mHolder;

    private static int mCurrSorting;
    private static boolean mIsDirectorySorting;

    public ChangeSorting(Activity act, boolean isDirectorySorting) {
        super(act.getApplicationContext());

        mIsDirectorySorting = isDirectorySorting;
        mListener = (ChangeDialogListener) act;
        mConfig = Config.newInstance(getContext());
        mHolder = act.getLayoutInflater().inflate(R.layout.change_sorting, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(act.getResources().getString(R.string.sort_by));
        builder.setView(mHolder);

        mCurrSorting = (mIsDirectorySorting ? mConfig.getDirectorySorting() : mConfig.getSorting());
        setupSortRadio();
        setupOrderRadio();

        builder.setPositiveButton(R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void setupSortRadio() {
        final RadioGroup sortingRadio = (RadioGroup) mHolder.findViewById(R.id.dialog_radio_sorting);
        RadioButton sortBtn = (RadioButton) sortingRadio.findViewById(R.id.dialog_radio_name);
        if ((mCurrSorting & Constants.SORT_BY_DATE) != 0) {
            sortBtn = (RadioButton) sortingRadio.findViewById(R.id.dialog_radio_date);
        } else if ((mCurrSorting & Constants.SORT_BY_SIZE) != 0) {
            sortBtn = (RadioButton) sortingRadio.findViewById(R.id.dialog_radio_size);
        }
        sortBtn.setChecked(true);
    }

    private void setupOrderRadio() {
        final RadioGroup orderRadio = (RadioGroup) mHolder.findViewById(R.id.dialog_radio_order);
        RadioButton orderBtn = (RadioButton) orderRadio.findViewById(R.id.dialog_radio_ascending);
        if ((mCurrSorting & Constants.SORT_DESCENDING) != 0) {
            orderBtn = (RadioButton) orderRadio.findViewById(R.id.dialog_radio_descending);
        }
        orderBtn.setChecked(true);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        final RadioGroup sortingRadio = (RadioGroup) mHolder.findViewById(R.id.dialog_radio_sorting);
        int sorting = Constants.SORT_BY_NAME;
        switch (sortingRadio.getCheckedRadioButtonId()) {
            case R.id.dialog_radio_date:
                sorting = Constants.SORT_BY_DATE;
                break;
            case R.id.dialog_radio_size:
                sorting = Constants.SORT_BY_SIZE;
                break;
            default:
                break;
        }

        final RadioGroup orderRadio = (RadioGroup) mHolder.findViewById(R.id.dialog_radio_order);
        if (orderRadio.getCheckedRadioButtonId() == R.id.dialog_radio_descending) {
            sorting |= Constants.SORT_DESCENDING;
        }

        if (mIsDirectorySorting) {
            if (mConfig.getDirectorySorting() != sorting) {
                mConfig.setDirectorySorting(sorting);
            }
        } else {
            if (mConfig.getSorting() != sorting) {
                mConfig.setSorting(sorting);
            }
        }
        mListener.sortingDialogClosed();
    }

    public interface ChangeDialogListener {
        void sortingDialogClosed();
    }
}

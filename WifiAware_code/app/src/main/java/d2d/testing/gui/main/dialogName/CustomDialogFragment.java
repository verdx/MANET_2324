package d2d.testing.gui.main.dialogName;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import d2d.testing.R;

public class CustomDialogFragment extends DialogFragment {

    private static final String TAG = "Dialog";
    private CustomDialogListener mListener;
    private String name = "defaultName";
    private EditText nameText;

    public CustomDialogFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CustomDialogListener) {
            mListener = (CustomDialogListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement CustomDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListener.onDialogNegative("defaultName");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View customView= inflater.inflate(R.layout.name_stream, null);
        nameText= (EditText) customView.findViewById(R.id.nameStream);

        builder.setView(customView)
                // Add action buttons
                .setPositiveButton(getString(R.string.save_str), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        if(mListener!=null){
                            if(validateForm()) {
                                mListener.onDialogPositive(name);
                            }
                            else mListener.onDialogPositive("defaultName");
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel_str), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CustomDialogFragment.this.getDialog().cancel();
                        if(mListener!=null){
                            mListener.onDialogNegative("defaultName");
                        }
                    }
                });
        return builder.create();
    }

    private boolean validateForm() {
        name = nameText.getText().toString();
        if(name.isEmpty()) return false;
        return true;
    }
}

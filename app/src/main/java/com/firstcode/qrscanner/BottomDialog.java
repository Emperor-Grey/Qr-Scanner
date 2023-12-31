package com.firstcode.qrscanner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomDialog extends BottomSheetDialogFragment {
    private String wifiInfo;
    private String fetchUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_dialog, container, false);

        TextView title = view.findViewById(R.id.Txt_File);
        TextView link = view.findViewById(R.id.Text_Link);
        Button btn_visit = view.findViewById(R.id.Visit);
        ImageView close = view.findViewById(R.id.Close);

        title.setText(fetchUrl);
        link.setText(wifiInfo);

        btn_visit.setOnClickListener(v -> {
            if (fetchUrl != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fetchUrl));
                startActivity(intent);
            }
        });

        close.setOnClickListener(v -> dismiss());

        return view;
    }

    public void setWifiInfo(String wifiInfo) {
        this.wifiInfo = wifiInfo;
    }

    public void setFetchUrl(String fetchUrl) {
        this.fetchUrl = fetchUrl;
    }
}

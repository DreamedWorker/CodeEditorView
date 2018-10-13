package com.summerain.widget;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import android.widget.VideoView;

public class MainActivity extends Activity 
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        CodeEditorView cev = (CodeEditorView)findViewById(R.id.a);
        cev.setText(readFileString("/storage/emulated/0/AppProjects/CodeEditorView/CodeView/app/src/main/java/com/summerain/widget/CodeEditorView.java"));


    }

    public String readFileString(String path)
    {
        try
        {
            String result="";
            BufferedReader iO=new BufferedReader(new FileReader(path));
            String str=iO.readLine();
            while (str != null)
            {
                result += str + "\n";
                str = iO.readLine();
            }
            return result;
        }
        catch (Exception e)
        {
            Log.e("file", "readFileString()->" + e.getMessage());
            return null;
        }
	}

}

package code_gs.binarycalculatortuner;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Point;

public class Calculator extends AppCompatActivity
{
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SAMPLE_RATE = 44100;
    private static final int BIT_SIZE = 3;
    private static final int FONT_SIZE = 125;
    private static final int COLUMN_NUMBER = 7;

    private Canvas m_Canvas;
    private Paint m_Paint;

    // Two numbers with BIT_SIZE digits and one sign
    private boolean[] m_tBits = new boolean[BIT_SIZE * 2 + 1];
    // Position of the selector box
    private int m_iPos = 0;
    // Last polled frequency
    private double m_dFrequency = 0;

    @Override
    protected void onCreate(Bundle instance)
    {
        super.onCreate(instance);
        setContentView(R.layout.activity_calculator);

        Point p = new Point();
        getWindowManager().getDefaultDisplay().getSize(p);

        // Full-screen
        Bitmap bitmap = Bitmap.createBitmap(
                p.x,
                p.y,
                Bitmap.Config.ALPHA_8 // Minimum colour selection
        );

        // Place the bitmap over the screen
        ((ImageView) findViewById(R.id.iv)).setImageBitmap(bitmap);

        m_Canvas = new Canvas(bitmap);
        m_Paint = new Paint();
        m_Paint.setStyle(Paint.Style.FILL);
        m_Paint.setColor(Color.WHITE);
        m_Paint.setAntiAlias(true); // Looks bad on Nexus without it. May just be the emulator
        m_Paint.setTextSize(FONT_SIZE);
        m_Paint.setTextAlign(Paint.Align.CENTER);

        final EditText min = (EditText) findViewById(R.id.pitch_one);
        final EditText max = (EditText) findViewById(R.id.pitch_two);

        // Polls the mic for the frequency and redraws
        ((Button) findViewById(R.id.btn_poll)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Will throw exceptions upon permission failure or too many apps using the mic
                try
                {
                    int size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING);
                    short[] buffer = new short[size];

                    AudioRecord audioInput = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNELS, ENCODING, size);
                    audioInput.startRecording();
                    int count = audioInput.read(buffer, 0, size);
                    audioInput.stop();
                    audioInput.release();
                    m_dFrequency = Tuner.getFrequency(SAMPLE_RATE, size, count, buffer);

                    // Change the currently selected bit if a valid frequency was returned
                    if (m_dFrequency != -1)
                        m_tBits[m_iPos] = !(Math.abs(m_dFrequency - Double.parseDouble(min.getText().toString())) < Math.abs(m_dFrequency - Double.parseDouble(max.getText().toString())));
                }
                catch (Exception e)
                {
                    //Log.d("Calculator", e.toString());
                    m_dFrequency = -1;
                }

                draw();
            }
        });

        ((Button) findViewById(R.id.btn_right)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                m_iPos = (m_iPos + 1) % m_tBits.length;
                draw();
            }
        });

        ((Button) findViewById(R.id.btn_left)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Wrap back around; Java's mod doesn't handle negatives properly >:(
                if (m_iPos == 0)
                    m_iPos = m_tBits.length - 1;
                else
                    m_iPos--;

                draw();
            }
        });

        draw();
    }

    /*private static String addBinary(String n1, String n2)
    {
        StringBuilder sb = new StringBuilder();
        int carry = 0;

        for (int i = BIT_SIZE - 1; i > -1; i--) {
            int sum = carry + n1.charAt(i) - '0' + n2.charAt(i) - '0';
            carry = sum >> 1;
            sb.append((sum & 1) == 0 ? '0' : '1');
        }

        if (carry > 0)
            sb.append('1');

        sb.reverse();

        return String.valueOf(sb);
    }*/

    void draw()
    {
        // Reset the canvas
        m_Canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        m_Canvas.drawText(
                String.valueOf(m_dFrequency),
                m_Canvas.getWidth() * (1.f/2),
                m_Canvas.getHeight() * (1.f/COLUMN_NUMBER),
                m_Paint
        );

        int n1 = 0;

        for (int i = 0; i < BIT_SIZE; i++)
        {
            if (i == m_iPos)
            {
                m_Paint.setFakeBoldText(true);
                m_Paint.setColor(Color.RED);
                m_Paint.setTextSize(FONT_SIZE * (7.f/6));
            }

            if (m_tBits[i])
                n1 += Math.pow(2, BIT_SIZE - 1 - i);

            m_Canvas.drawText(
                    m_tBits[i] ? "1" : "0",
                    m_Canvas.getWidth() * ((i + 1.f)/(BIT_SIZE + 1)),
                    m_Canvas.getHeight() * (2.f/COLUMN_NUMBER),
                    m_Paint
            );

            if (i == m_iPos)
            {
                m_Paint.setFakeBoldText(false);
                m_Paint.setColor(Color.WHITE);
                m_Paint.setTextSize(FONT_SIZE);
            }
        }

        if (m_iPos == BIT_SIZE)
        {
            m_Paint.setFakeBoldText(true);
            m_Paint.setColor(Color.RED);
            m_Paint.setTextSize(FONT_SIZE * (7.f/6));
        }

        m_Canvas.drawText(
                m_tBits[BIT_SIZE] ? "-" : "+",
                m_Canvas.getWidth() * (1.f/2),
                m_Canvas.getHeight() * (3.f/COLUMN_NUMBER),
                m_Paint
        );

        if (m_iPos == BIT_SIZE)
        {
            m_Paint.setFakeBoldText(false);
            m_Paint.setColor(Color.WHITE);
            m_Paint.setTextSize(FONT_SIZE);
        }

        int n2 = 0;

        for (int i = BIT_SIZE + 1; i < m_tBits.length; i++)
        {
            if (i == m_iPos)
            {
                m_Paint.setFakeBoldText(true);
                m_Paint.setColor(Color.RED);
                m_Paint.setTextSize(FONT_SIZE * (7.f/6));
            }

            if (m_tBits[i])
                n2 += Math.pow(2, BIT_SIZE - (i - BIT_SIZE));

            m_Canvas.drawText(
                    m_tBits[i] ? "1" : "0",
                    m_Canvas.getWidth() * ((float)(i - BIT_SIZE)/(BIT_SIZE + 1)), // x
                    m_Canvas.getHeight() * (4.f/COLUMN_NUMBER),
                    m_Paint
            );

            if (i == m_iPos)
            {
                m_Paint.setFakeBoldText(false);
                m_Paint.setColor(Color.WHITE);
                m_Paint.setTextSize(FONT_SIZE);
            }
        }

        m_Canvas.drawText(
                "=",
                m_Canvas.getWidth() * (1.f/2),
                m_Canvas.getHeight() * (5.f/COLUMN_NUMBER),
                m_Paint
        );

        int output = n1 + (m_tBits[BIT_SIZE] ? -n2 : n2);
        
        if (output < 0)
            output += Math.pow(2, BIT_SIZE + 1);
        //else
           // output %= BIT_SIZE + 1;

        m_Canvas.drawText(
                Integer.toBinaryString(output),
                m_Canvas.getWidth() * (1.f/2),
                m_Canvas.getHeight() * (6.f/COLUMN_NUMBER),
                m_Paint
        );
    }
}

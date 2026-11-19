package ar.trazagrupos.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,48,32,32); root.setBackgroundColor(Color.rgb(13,29,28));
        TextView title=new TextView(this);title.setText("🗨 TrazaGrupos");title.setTextColor(Color.WHITE);title.setTextSize(26);root.addView(title);
        TextView detail=new TextView(this);detail.setText("Panel flotante para encontrar y agrupar mensajes visibles de WhatsApp. El análisis es local. El servicio solo escucha WhatsApp y puede desactivarse desde Ajustes. Para cargar mensajes anteriores, desplazate por el chat.\n\nLa lectura de números y autores depende de lo que WhatsApp muestre a Android. Revisá cualquier respuesta antes de enviarla.");detail.setTextColor(0xffc4d4ce);detail.setTextSize(16);detail.setPadding(0,24,0,24);root.addView(detail);
        Button settings=new Button(this);settings.setText("Activar servicio de accesibilidad");settings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));root.addView(settings);
        TextView foot=new TextView(this);foot.setText("Luego abrí un grupo de WhatsApp. Aparecerá un botón flotante para consultar los mensajes visibles.");foot.setTextColor(0xff8fc9b6);foot.setPadding(0,24,0,0);root.addView(foot);
        setContentView(root);
    }
}

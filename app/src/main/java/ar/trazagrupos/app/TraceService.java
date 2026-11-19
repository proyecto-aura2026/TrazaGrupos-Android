package ar.trazagrupos.app;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TraceService extends AccessibilityService {
    private static final int DARK=0xe8192928, WHITE=0xffe9edef, GREEN=0xff13c79b;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final LinkedHashMap<String,LinkedHashMap<String,Record>> groups=new LinkedHashMap<>();
    private WindowManager wm; private View widget,panel; private WindowManager.LayoutParams widgetParams,panelParams;
    private LinearLayout list; private TextView count; private EditText query,draft;
    private String selectedTopic="Todos",selectedPerson="Todos",selectedGroup=null,visibleGroup=null; private long lastScan;
    private final String[] topics={"Todos","Web e IA","API","Suscripciones","Coordinación","Otros"};
    private static class Record { String text,person,topic,chat; long seen; Record(String t,String p,String g,String c){text=t;person=p;topic=g;chat=c;seen=System.currentTimeMillis();} }
    private static class NodeText {String value; Rect bounds; NodeText(String v,Rect b){value=v;bounds=b;}}
    private LinkedHashMap<String,Record> records(){return selectedGroup==null?new LinkedHashMap<>():groups.computeIfAbsent(selectedGroup,k->new LinkedHashMap<>());}

    @Override protected void onServiceConnected(){super.onServiceConnected();wm=(WindowManager)getSystemService(WINDOW_SERVICE);showWidget();}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e==null||e.getPackageName()==null)return;
        String pkg=e.getPackageName().toString();if(!pkg.equals("com.whatsapp")&&!pkg.equals("com.whatsapp.w4b"))return;
        long now=System.currentTimeMillis();if(now-lastScan<300)return;lastScan=now;
        handler.postDelayed(this::scan,320);
    }
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){remove(panel);remove(widget);super.onDestroy();}
    private void scan(){
        ArrayList<NodeText> nodes=visibleNodes();visibleGroup=chatTitle(nodes);
        if(selectedGroup==null||visibleGroup==null||!selectedGroup.equals(visibleGroup)){if(panel!=null)renderList();return;}
        LinkedHashMap<String,Record> target=records();
        String person="Autor no identificado";int personY=-10000;int h=getResources().getDisplayMetrics().heightPixels;
        Collections.sort(nodes,(a,b)->Integer.compare(a.bounds.top,b.bounds.top));
        for(NodeText n:nodes){String s=n.value;
            if(n.bounds.top<h*.13||n.bounds.bottom>h*.91)continue;
            Matcher number=Pattern.compile("\\+\\d[\\d ()-]{7,20}").matcher(s);
            if(number.find()&&s.length()<90){person=number.group().trim();personY=n.bounds.top;continue;}
            if(s.length()<12||s.length()>1800||s.matches("(?i).*(escribe un mensaje|buscar|videollamada|más opciones|añadir estado|cifrad[oa]s de extremo|enviar historial).*"))continue;
            if(s.matches("(?i)^(ayer|hoy|mensaje|en línea|\\d{1,2}:\\d{2}.*)$"))continue;
            String author=n.bounds.top-personY<h*.075?person:"Autor no identificado";
            String key=author+"|"+s;
            if(!target.containsKey(key)){target.put(key,new Record(s,author,classify(s),selectedGroup));if(target.size()>500)target.remove(target.keySet().iterator().next());}
        }
        if(panel!=null)renderList();
    }
    private ArrayList<NodeText> visibleNodes(){
        ArrayList<NodeText> out=new ArrayList<>();List<AccessibilityWindowInfo> windows=getWindows();
        if(windows==null)return out;
        for(AccessibilityWindowInfo win:windows){AccessibilityNodeInfo root=win.getRoot();if(root==null)continue;
            CharSequence pkg=root.getPackageName();if(pkg!=null&&(pkg.toString().equals("com.whatsapp")||pkg.toString().equals("com.whatsapp.w4b")))walk(root,out,0);
            root.recycle();
        }return out;
    }
    private void walk(AccessibilityNodeInfo n,ArrayList<NodeText> out,int depth){
        if(n==null||depth>35||out.size()>350)return;
        int count=n.getChildCount();CharSequence text=n.getText();
        if(count==0&&text!=null){String s=text.toString().trim();Rect r=new Rect();n.getBoundsInScreen(r);if(!s.isEmpty()&&r.height()>0)out.add(new NodeText(s,r));}
        for(int i=0;i<count;i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){walk(c,out,depth+1);c.recycle();}}
    }
    private String chatTitle(ArrayList<NodeText> nodes){int w=getResources().getDisplayMetrics().widthPixels,h=getResources().getDisplayMetrics().heightPixels;
        for(NodeText n:nodes){Rect r=n.bounds;String s=n.value;
            if(r.top>=h*.045&&r.top<h*.12&&r.left>w*.22&&r.left<w*.8&&s.length()>3&&s.length()<90&&!s.matches("(?i).*(en línea|online|participantes|miembros).*"))return s;
        }return null;
    }
    private String classify(String s){String t=s.toLowerCase(Locale.ROOT);
        if(t.contains("api")||t.contains("deepseek")||t.contains("tokens"))return "API";
        if(t.contains("web")||t.contains("codex")||t.contains("ia")||t.contains("html")||t.contains("apk"))return "Web e IA";
        if(t.contains("suscrip")||t.contains("cuenta")||t.contains("venta"))return "Suscripciones";
        if(t.contains("horario")||t.contains("reunión")||t.contains("fecha"))return "Coordinación";
        return "Otros";
    }
    private WindowManager.LayoutParams params(int w,int h,int x,int y){WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;p.x=x;p.y=y;return p;}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);d.setStroke(1,0x66ffffff);return d;}
    private TextView label(String t,int size){TextView v=new TextView(this);v.setText(t);v.setTextColor(WHITE);v.setTextSize(size);v.setPadding(10,8,10,8);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setTextColor(WHITE);b.setAllCaps(false);b.setBackground(bg(0xff25433b,14));return b;}
    private void showWidget(){if(widget!=null)return;Button b=button("🗨 Trazar");widget=b;widgetParams=params(150,65,20,180);wm.addView(b,widgetParams);b.setOnClickListener(v->togglePanel());drag(b,widgetParams);}
    private void togglePanel(){if(panel!=null){remove(panel);panel=null;return;}showPanel();}
    private void showPanel(){
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setPadding(7,7,7,7);shell.setBackground(bg(DARK,26));panel=shell;
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);TextView title=label("🗨 Trazabilidad",15);title.setTypeface(null,Typeface.BOLD);head.addView(title,new LinearLayout.LayoutParams(0,55,1));Button close=button("×");head.addView(close,new LinearLayout.LayoutParams(55,50));close.setOnClickListener(v->togglePanel());shell.addView(head);drag(head,null);
        Button choose=button("Elegir este grupo de WhatsApp");shell.addView(choose);choose.setOnClickListener(v->{
            String titleNow=chatTitle(visibleNodes());if(titleNow==null){count.setText("Abrí un grupo de WhatsApp antes de elegirlo.");return;}
            selectedGroup=titleNow;selectedPerson="Todos";selectedTopic="Todos";query.setText("");scan();renderList();
        });
        count=label("Elegí un grupo para iniciar la trazabilidad",11);shell.addView(count);
        LinearLayout controls=new LinearLayout(this);Button person=button("Número: Todos");Button topic=button("Tema: Todos");controls.addView(person,new LinearLayout.LayoutParams(0,50,1));controls.addView(topic,new LinearLayout.LayoutParams(0,50,1));shell.addView(controls);
        person.setOnClickListener(v->{ArrayList<String> all=new ArrayList<>();all.add("Todos");for(Record r:records().values())if(!all.contains(r.person))all.add(r.person);selectedPerson=all.get((all.indexOf(selectedPerson)+1)%all.size());person.setText("Número: "+selectedPerson);renderList();});
        topic.setOnClickListener(v->{int n=0;for(int i=0;i<topics.length;i++)if(topics[i].equals(selectedTopic))n=i;selectedTopic=topics[(n+1)%topics.length];topic.setText("Tema: "+selectedTopic);renderList();});
        query=new EditText(this);query.setSingleLine(true);query.setTextColor(WHITE);query.setHintTextColor(0xff9bb6ab);query.setHint("Buscar mensajes");shell.addView(query);query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){renderList();}public void afterTextChanged(Editable e){}});
        ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        draft=new EditText(this);draft.setTextColor(WHITE);draft.setHintTextColor(0xff9bb6ab);draft.setHint("Redactar respuesta y copiar para revisar en WhatsApp");draft.setMinLines(2);shell.addView(draft);
        Button copy=button("Copiar borrador · envío manual");shell.addView(copy);copy.setOnClickListener(v->{String s=draft.getText().toString();if(!s.trim().isEmpty()){ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Borrador TrazaGrupos",s));copy.setText("Copiado. Revisá antes de enviar");}});
        int width=getResources().getDisplayMetrics().widthPixels;int height=getResources().getDisplayMetrics().heightPixels;
        panelParams=params(Math.min(width-24,550),Math.min((int)(height*.66),1000),12,(int)(height*.22));panelParams.flags=WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;wm.addView(shell,panelParams);renderList();
    }
    private void renderList(){if(list==null||count==null)return;list.removeAllViews();String q=query==null?"":query.getText().toString().toLowerCase(Locale.ROOT);int n=0;
        for(Record r:records().values()){if(!selectedTopic.equals("Todos")&&!selectedTopic.equals(r.topic))continue;if(!selectedPerson.equals("Todos")&&!selectedPerson.equals(r.person))continue;if(!r.text.toLowerCase(Locale.ROOT).contains(q))continue;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(bg(0xff263b38,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,8);list.addView(card,lp);
            TextView meta=label(r.person+" · "+r.topic,11);meta.setTextColor(GREEN);card.addView(meta);TextView body=label(r.text,13);card.addView(body);n++;
        }count.setText(selectedGroup==null?"Elegí un grupo de WhatsApp para iniciar.":n+" mensajes · "+selectedGroup+(selectedGroup.equals(visibleGroup)?" · grupo activo":" · otro chat abierto; captura pausada"));
    }
    private void drag(View handle,WindowManager.LayoutParams explicit){handle.setOnTouchListener(new View.OnTouchListener(){float x0,y0;int startX,startY;boolean moved;public boolean onTouch(View v,MotionEvent e){WindowManager.LayoutParams p=explicit!=null?explicit:panelParams;if(p==null)return false;switch(e.getAction()){case MotionEvent.ACTION_DOWN:x0=e.getRawX();y0=e.getRawY();startX=p.x;startY=p.y;moved=false;return true;case MotionEvent.ACTION_MOVE:if(Math.abs(e.getRawX()-x0)+Math.abs(e.getRawY()-y0)>10)moved=true;if(moved){p.x=startX+(int)(e.getRawX()-x0);p.y=startY+(int)(e.getRawY()-y0);wm.updateViewLayout(explicit!=null?widget:panel,p);}return true;case MotionEvent.ACTION_UP:if(!moved)v.performClick();return true;}return false;}});}
    private void remove(View v){if(v!=null&&wm!=null){try{wm.removeView(v);}catch(Exception ignored){}}}
}

package com.example.vtubersubs

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import java.util.regex.Pattern

class MainActivity : Activity() {
 data class Cue(val start:Long,val end:Long,val text:String)
 private val cues= mutableListOf<Cue>()
 private lateinit var web:WebView
 private lateinit var subtitle:TextView
 private lateinit var root:FrameLayout
 private lateinit var controls:LinearLayout
 private val handler=Handler(Looper.getMainLooper())
 private var offsetMs=0L
 private var pip=false
 private val openFile=120
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  root=FrameLayout(this).apply{setBackgroundColor(Color.BLACK)}
  val column=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  controls=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,8,12,8);setBackgroundColor(Color.rgb(25,25,25))}
  val url=EditText(this).apply{hint="유튜브 링크 또는 영상 ID";setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)}
  controls.addView(url)
  val buttons=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  fun button(label:String, action:()->Unit){buttons.addView(Button(this).apply{text=label;setOnClickListener{action()}},LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))}
  button("재생") {val id=videoId(url.text.toString());if(id==null)Toast.makeText(this,"올바른 유튜브 링크를 입력하세요",Toast.LENGTH_SHORT).show() else web.evaluateJavascript("loadVideo('${id}')",null)}
  button("SRT/VTT") {startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("text/plain","application/x-subrip","text/vtt"))},openFile)}
  button("-0.5초") {offsetMs-=500}
  button("+0.5초") {offsetMs+=500}
  controls.addView(buttons)
  val second=LinearLayout(this)
  fun secondButton(label:String,action:()->Unit){second.addView(Button(this).apply{text=label;setOnClickListener{action()}},LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))}
  secondButton("전체화면") {controls.visibility=if(controls.visibility==View.VISIBLE)View.GONE else View.VISIBLE;window.decorView.systemUiVisibility=if(controls.visibility==View.GONE)5894 or 1024 or 512 else 0}
  secondButton("PiP 실험") {enterPip()}
  controls.addView(second)
  column.addView(controls)
  web=WebView(this).apply{settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.mediaPlaybackRequiresUserGesture=true;webChromeClient=WebChromeClient();webViewClient=WebViewClient();loadUrl("https://localhost/player.html")}
  // Local HTML is loaded via a WebViewAssetLoader-like intercept to preserve a secure origin.
  web.webViewClient=object:WebViewClient(){override fun shouldInterceptRequest(view:WebView?,request:android.webkit.WebResourceRequest?):android.webkit.WebResourceResponse?{
    if(request?.url?.host=="localhost" && request.url.path=="/player.html")return android.webkit.WebResourceResponse("text/html","UTF-8",assets.open("player.html"))
    return super.shouldInterceptRequest(view,request)
  }}
  column.addView(web,LinearLayout.LayoutParams(-1,0,1f));root.addView(column)
  subtitle=TextView(this).apply{setTextColor(Color.WHITE);setBackgroundColor(0xB0000000.toInt());textSize=18f;gravity=Gravity.CENTER;setPadding(12,6,12,6);visibility=View.GONE}
  root.addView(subtitle,FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM).apply{bottomMargin=20})
  setContentView(root)
  handler.post(object:Runnable{override fun run(){if(!isFinishing) {web.evaluateJavascript("currentTime()") { result -> val seconds=result.trim('"').toDoubleOrNull()?:0.0;val time=(seconds*1000).toLong()+offsetMs;val cue=cues.firstOrNull{time>=it.start&&time<it.end};subtitle.text=cue?.text.orEmpty();subtitle.visibility=if(cue==null)View.GONE else View.VISIBLE };handler.postDelayed(this,250)}}})
 }
 private fun videoId(s:String):String? {val p=Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?[^#]*?v=|shorts/|live/|embed/))([A-Za-z0-9_-]{11})");val m=p.matcher(s);return if(m.find())m.group(1) else s.trim().takeIf{it.matches(Regex("[A-Za-z0-9_-]{11}"))}}
 @Deprecated("Activity result") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==openFile&&resultCode==RESULT_OK){val uri=data?.data?:return;val txt=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:return;cues.clear();cues.addAll(parseCues(txt));Toast.makeText(this,"자막 ${cues.size}개 불러옴",Toast.LENGTH_SHORT).show()}}
 private fun parseCues(raw:String):List<Cue>{val normalized=raw.replace("\r\n","\n").removePrefix("\uFEFF");val blocks=normalized.split(Regex("\n\\s*\n"));val regex=Regex("(\\d{1,2}:)?(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{1,2}:)?(\\d{2}):(\\d{2})[,.](\\d{3})")
  fun ms(h:String?,m:String,s:String,milli:String):Long=(((h?.removeSuffix(":")?.toLongOrNull()?:0)*3600+m.toLong()*60+s.toLong())*1000)+milli.toLong()
  return blocks.mapNotNull{block->val lines=block.lines();val index=lines.indexOfFirst{regex.containsMatchIn(it)};if(index<0)null else {val match=regex.find(lines[index])!!;val g=match.groupValues;val start=ms(g[1],g[2],g[3],g[4]);val end=ms(g[5],g[6],g[7],g[8]);val text=lines.drop(index+1).joinToString("\n").trim();if(text.isEmpty())null else Cue(start,end,text)}}.sortedBy{it.start}
 }
 private fun enterPip(){if(Build.VERSION.SDK_INT>=26){try{enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16,9)).build())}catch(e:Exception){Toast.makeText(this,"PiP 진입 실패: ${e.message}",Toast.LENGTH_LONG).show()}}}
 override fun onPictureInPictureModeChanged(isInPictureInPictureMode:Boolean,newConfig:Configuration){super.onPictureInPictureModeChanged(isInPictureInPictureMode,newConfig);pip=isInPictureInPictureMode;controls.visibility=if(pip)View.GONE else View.VISIBLE;subtitle.textSize=if(pip)11f else 18f}
 override fun onDestroy(){handler.removeCallbacksAndMessages(null);web.destroy();super.onDestroy()}
}

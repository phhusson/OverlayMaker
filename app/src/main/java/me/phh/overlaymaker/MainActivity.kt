package me.phh.overlaymaker

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    fun delete(f: File) {
        if (f.isDirectory) {
            for (c in f.listFiles()) delete(c)
        }
        f.delete()
    }

    fun dumpPowerprofile(overlay: File, resources: Resources) {
        val tgtFolder = File(File(overlay, "res"), "xml")
        tgtFolder.mkdirs()

        val powerProfileId = resources.getIdentifier("power_profile", "xml", "android")
        val xml = resources.getXml(powerProfileId)
        val generatedXml = StringBuilder()
        var eventType = xml.eventType
        var depth = 0
        while(eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_DOCUMENT) {
                Log.d("PHH", "Start document")
            } else if(eventType == XmlPullParser.START_TAG) {
                var str = (" ".repeat(depth)) + "<" + xml.name
                for(i in 0 until xml.attributeCount) {
                    val attrName = xml.getAttributeName(i)
                    val attrValue = xml.getAttributeValue(i)
                    str += " $attrName=\"$attrValue\""
                }
                str += ">"
                generatedXml.append(str)
                depth++
            } else if(eventType == XmlPullParser.END_TAG) {
                depth--
                generatedXml.append("</${xml.name}>\n")
            } else if(eventType == XmlPullParser.TEXT) {
                generatedXml.append(xml.text)
            }
            eventType = xml.next()
        }
        FileOutputStream(File(tgtFolder, "power_profile.xml")).writer().use {
            it.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            it.write(generatedXml.toString())
            it.close()
        }
    }

    fun dumpArray(resources: Resources, name: String): String? {
        val sb = StringBuilder()
        sb.append("<array name=\"$name\">")
        val resId = resources.getIdentifier(name, "array", "android")

        try {
            val arr = resources.getTextArray(resId)
            if(arr.isEmpty()) throw Exception()
            for (i in arr) {
                sb.append("<item>${i!!}</item>")
            }
        } catch(e: Throwable) {
            val arr = resources.getIntArray(resId)
            if(arr.isEmpty()) return null
            for (i in arr) {
                sb.append("<item>$i</item>")
            }
        }
        sb.append("</array>")
        return sb.toString()
    }

    fun getDimen(resources: Resources, name: String): Float? {
        val resId = resources.getIdentifier(name, "dimen", "android")
        return resources.getDimension(resId)
    }

    fun getString(resources: Resources, name: String): String? {
        val resId = resources.getIdentifier(name, "string", "android")
        return resources.getString(resId)
    }

    fun dumpBrightness(overlay: File, resources: Resources) {
        val tgtFolder = File(File(overlay, "res"), "values")
        tgtFolder.mkdirs()

        val writer = FileOutputStream(File(tgtFolder, "brightness.xml")).writer()
        writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        writer.write("<resources>")

        val s = dumpArray(resources, "config_autoBrightnessDisplayValuesNits")
        if(s != null) {
            writer.write(s)
            writer.write("\n")
            writer.write(dumpArray(resources, "config_screenBrightnessNits"))
            writer.write("\n")
            writer.write(dumpArray(resources, "config_screenBrightnessBacklight"))
            writer.write("\n")
            writer.write(dumpArray(resources, "config_autoBrightnessLevels"))
            writer.write("\n")
        }
        writer.write("</resources>")
        writer.close()
    }

    fun dumpNotch(overlay: File, resources: Resources) {
        val tgtFolder = File(File(overlay, "res"), "values")
        tgtFolder.mkdirs()

        val writer = FileOutputStream(File(tgtFolder, "notch.xml")).writer()
        writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        writer.write("<resources>\n")

        val statusBarFloat = try {
            getDimen(resources, "status_bar_height_portrait")!!
        } catch(t: Throwable) {
            getDimen(resources, "status_bar_height")!!
        }
        val statusBar = String.format("%.00f", statusBarFloat)
        writer.write("<dimen name=\"status_bar_height_portrait\">$statusBar</dimen>\n")
        writer.write("<dimen name=\"status_bar_height\">$statusBar</dimen>\n")
        writer.write("<dimen name=\"status_bar_height_landscape\">24dp</dimen>\n")

        val cutout = getString(resources, "config_mainBuiltInDisplayCutout")
        writer.write("<dimen name=\"config_mainBuiltInDisplayCutout\">$cutout</dimen>\n")
        writer.write("</resources>\n")
        writer.close()

        if (true) {
            val tgtFolder = File(File(overlay, "res"), "values-land")
            tgtFolder.mkdirs()

            val writer = FileOutputStream(File(tgtFolder, "notch.xml")).writer()
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            writer.write("<resources>\n")
            writer.write("<dimen name=\"status_bar_height\">24dp</dimen>\n")
            writer.write("</resources>\n")
            writer.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val androidResources = packageManager.getResourcesForApplication("android")
        val overlay = File(externalCacheDir, "overlay")

        // Cleanup overlay folder
        delete(overlay)

        overlay.mkdirs()
        dumpPowerprofile(overlay, androidResources)
        dumpBrightness(overlay, androidResources)
        dumpNotch(overlay, androidResources)
    }
}
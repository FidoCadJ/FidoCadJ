package fidocadj.globals;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/** Taken here http://www.thoughtsabout.net/blog/archives/000044.html
    TODO: review Javadoc comments, check the license.
*/
public final class Utf8ResourceBundle
{
    /** Standard creator.
    */
    private Utf8ResourceBundle()
    {
        // Nothing to do.
    }

    /** Get the resource bundle.
        @param baseName the name of the bundle to be retrieved.
        @return the resource bundle object.
    */
    public static ResourceBundle getBundle(String baseName)
    {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName);
        return createUtf8PropertyResourceBundle(bundle);
    }

    /** Get the resource bundle.
        @param baseName the name of the bundle to be retrieved.
        @param locale the locale to be searched for.
        @return the resource bundle object.
    */
    public static ResourceBundle getBundle(String baseName,
        Locale locale)
    {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
        return createUtf8PropertyResourceBundle(bundle);
    }

    /** Get the resource bundle.
        @param baseName the name of the bundle to be retrieved.
        @param locale the locale to be searched for.
        @param loader the loader class to which the bundle should be
            associated.
        @return the resource bundle object.
    */
    public static ResourceBundle getBundle(String baseName, Locale locale,
        ClassLoader loader)
    {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale,
            loader);
        return createUtf8PropertyResourceBundle(bundle);
    }

    /** Create a resource bundle with proper handling of UTF-8 resources.
        @param bundle the original bundle.
        @return the resource bundle.
    */
    private static ResourceBundle createUtf8PropertyResourceBundle(
        ResourceBundle bundle)
    {
        if (!(bundle instanceof PropertyResourceBundle)) {
            return bundle;
        }
        return new Utf8PropertyResourceBundle((PropertyResourceBundle)bundle);
    }

    private static class Utf8PropertyResourceBundle extends ResourceBundle
    {
        PropertyResourceBundle bundle;

        public Utf8PropertyResourceBundle(PropertyResourceBundle bundle)
        {
            this.bundle = bundle;
        }
        /* (non-Javadoc)
        * @see java.util.ResourceBundle#getKeys()
        */
        @Override public Enumeration<String> getKeys()
        {
            return bundle.getKeys();
        }
        /* (non-Javadoc)
        * @see java.util.ResourceBundle#handleGetObject(java.lang.String)
        */
        @Override protected Object handleGetObject(String key)
        {
            String value = bundle.getString(key);
            String version = System.getProperty("java.specification.version");

            // Things have changed starting from Java 9: the bundle.getString
            // returns directly an UTF-8 string, thus the translation is not
            // required anymore (JEP 226).
            if("1.7".equals(version) || "1.8".equals(version)) {
                // FindBugs suggests the following test is redundant.
                //if (value==null) return null;
                try {
                    return new String (value.getBytes("ISO-8859-1"),"UTF-8") ;
                } catch (UnsupportedEncodingException e) {
                    System.err.println("Unsupported encoding. "+
                        "Problems in Utf8PropertyResourceBundle class.");
                    return null;
                }
            } else {
                return value;
            }
        }
    }
}
package com.halcyon.authservice.util;

import com.halcyon.authservice.exception.CookieDeserializationException;
import jakarta.servlet.http.Cookie;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Base64;

public class DeserializationUtil {
    private DeserializationUtil() {}

    public static <T> T deserialize(Cookie cookie, Class<T> targetClass) {
       try {
           byte[] data = Base64.getUrlDecoder().decode(cookie.getValue());
           ByteArrayInputStream bis = new ByteArrayInputStream(data);
           ObjectInputStream ois = new ObjectInputStream(bis);

           Object object = ois.readObject();
           ois.close();

           return targetClass.cast(object);
       } catch (IOException | ClassNotFoundException e) {
           throw new CookieDeserializationException(e);
       }
    }
}

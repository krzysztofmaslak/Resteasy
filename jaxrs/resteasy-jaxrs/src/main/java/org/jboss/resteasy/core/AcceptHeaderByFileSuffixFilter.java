package org.jboss.resteasy.core;

import org.jboss.resteasy.specimpl.PathSegmentImpl;
import org.jboss.resteasy.util.LocaleHelper;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modifies Accept and Accept-Language headers by looking at path file suffix i.e. .xml means Accept application/xml
 */
@Provider
@PreMatching
public class AcceptHeaderByFileSuffixFilter implements ContainerRequestFilter
{
   public Map<String, MediaType> mediaTypeMappings = new HashMap<String, MediaType>();
   public Map<String, String> languageMappings = new HashMap<String, String>();

   public void setMediaTypeMappings(Map<String, MediaType> mediaTypeMappings)
   {
      this.mediaTypeMappings = mediaTypeMappings;
   }

   public void setLanguageMappings(Map<String, String> languageMappings)
   {
      this.languageMappings = languageMappings;
   }

   public Map<String, MediaType> getMediaTypeMappings()
   {
      return mediaTypeMappings;
   }

   public Map<String, String> getLanguageMappings()
   {
      return languageMappings;
   }

   @Override
   public void filter(ContainerRequestContext requestContext) throws IOException
   {
      List<PathSegment> segments = null;
      if (mediaTypeMappings != null || languageMappings != null)
      {
         segments = process(requestContext, segments);
      }
      if (segments == null)
      {
         return;
      }

      StringBuilder preprocessedPath = new StringBuilder();
      for (PathSegment pathSegment : segments)
      {
         preprocessedPath.append("/").append(pathSegment.getPath());
      }
      URI requestUri = URI.create(preprocessedPath.toString());
      requestContext.setRequestUri(requestUri);

   }

   private List<PathSegment> process(ContainerRequestContext in, List<PathSegment> segments)
   {
      String path = in.getUriInfo().getPath(false);
      int lastSegment = path.lastIndexOf('/');
      if (lastSegment < 0)
      {
         lastSegment = 0;
      }
      int index = path.indexOf('.', lastSegment);
      if (index < 0)
      {
         return null;
      }

      boolean preprocessed = false;

      String extension = path.substring(index + 1);
      String[] extensions = extension.split("\\.");

      StringBuilder rebuilt = new StringBuilder(path.substring(0, index));
      for (String ext : extensions)
      {
         if (mediaTypeMappings != null)
         {
            MediaType match = mediaTypeMappings.get(ext);
            if (match != null)
            {
               in.getAcceptableMediaTypes().add(0, match);
               preprocessed = true;
               continue;
            }
         }
         if (languageMappings != null)
         {
            String match = languageMappings.get(ext);
            if (match != null)
            {
               in.getAcceptableLanguages().add(
                       LocaleHelper.extractLocale(match));
               preprocessed = true;
               continue;
            }
         }
         rebuilt.append(".").append(ext);
      }
      if (preprocessed)
      {
         segments = PathSegmentImpl.parseSegments(rebuilt.toString(), false);
      }
      return segments;
   }

}

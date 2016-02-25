package com.sonymobile.jenkins.plugins.lenientshutdown;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

@Extension
public class ShutdownConfiguration extends GlobalConfiguration
{
  
  public static final String DELIMETER = "\\r?\\n";
  
  /**
   * Defines the default shutdown message to be displayed in header.
   */
  private String shutdownMessage = Messages.GoingToShutDown();
  
  
  private boolean allowAllQueuedItems;
  
  /**
   * A list of projects that are allowed to run in case allowAllQueuedItems is enabled
   */
  private Set<String> whiteListedProjects = new TreeSet<String>();

  
  public ShutdownConfiguration()
  {
    load();
  }
  
  /**
   * Checks if all queued items are allowed to build in lenient shutdown mode 
   * @return true if all queued itmes will build, false otherwise
   */
  public boolean isAllowAllQueuedItems()
  {
    return allowAllQueuedItems;
  }
  
  /**
   * Sets the flag if all queued items are allowed to finish or not
   * @param allowAllQueuedItems
   */
  public void setAllowAllQueuedItems(boolean allowAllQueuedItems)
  {
    this.allowAllQueuedItems = allowAllQueuedItems;
  }
  
  /**
   * Gets the shutdown message to be displayed in header.
   * @return message to display in header
   */
  public String getShutdownMessage() {
      return shutdownMessage;
  }

  /**
   * Sets the shutdown message to be displayed in header.
   * @param shutdownMessage message to display in header
   */
  public void setShutdownMessage(String shutdownMessage) {
      this.shutdownMessage = shutdownMessage;
  }
  
  /**
   * Gets the white listed projects as a string
   * @return string with the white listed projects separated by newlines
   */
  public String getWhiteListedProjectsText()
  {
    return StringUtils.join(whiteListedProjects, "\n");
  }

  /**
   * Called when an admin saves settings in the global configuration page.
   * Persists the current settings to disk.
   * @param staplerRequest the request
   * @param json form data
   * @return always true
   * @throws FormException if the form was malformed
   */
  @Override
  public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
      shutdownMessage = json.getString("shutdownMessage");
      allowAllQueuedItems = json.getBoolean("allowAllQueuedItems");
      whiteListedProjects.clear();
      whiteListedProjects.addAll(Arrays.asList(json.getString("whiteListedProjects").split(DELIMETER)));
      save();
      return true;
  }

  public static ShutdownConfiguration getInstance()
  {
    return GlobalConfiguration.all().get(ShutdownConfiguration.class);
  }
}

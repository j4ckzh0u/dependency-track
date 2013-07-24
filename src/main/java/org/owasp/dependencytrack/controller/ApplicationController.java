/*
 * Copyright 2013 OWASP Foundation
 *
 * This file is part of OWASP Dependency-Track.
 *
 * Dependency-Track is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Dependency-Track is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Dependency-Track.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.owasp.dependencytrack.controller;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.owasp.dependencytrack.model.*;
import org.owasp.dependencytrack.service.ApplicationService;
import org.owasp.dependencytrack.service.ApplicationVersionService;
import org.owasp.dependencytrack.service.LibraryVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Controller
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationVersionService applicationVersionService;

    @Autowired
    private LibraryVersionService libraryVersionService;


    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String loginchk(@RequestParam("username") String username,
                           @RequestParam("password") String passwd, ModelMap modelMap) {
        UsernamePasswordToken token = new UsernamePasswordToken(username,
                passwd);
        try {
            SecurityUtils.getSubject().login(token);
        } catch (AuthenticationException e) {

        }
        return "redirect:/login";

    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(ModelMap modelMap) {
        String s = "login";
        if (SecurityUtils.getSubject().isAuthenticated()) {
            s = "redirect:/home";
        }
        return s;
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(ModelMap modelMap) {
        SecurityUtils.getSubject().logout();
        return "redirect:/login";
    }

    /*
     * Default page
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String application() {
        return "redirect:/applications";
    }

    /*
     * Lists all applications
     */
    @RequestMapping(value = "/applications", method = RequestMethod.GET)
    public String application(Map<String, Object> map) {
        map.put("application", new Application());
        map.put("applicationList", applicationService.listApplications());
        return "applicationsPage";
    }

    /*
     * Adds an application and associated version number
     */
    @RequestMapping(value = "/addApplication", method = RequestMethod.POST)
    public String addApplication(@ModelAttribute("application") Application application,
                                 BindingResult result, @RequestParam("version") String version) {
        applicationService.addApplication(application, version);
        return "redirect:/applications";
    }

    /*
     * Updates an applications' name
     */
    @RequestMapping(value = "/updateApplication", method = RequestMethod.POST)
    public String updatingProduct(@RequestParam("id") int id, @RequestParam("name") String name) {
        applicationService.updateApplication(id, name);
        return "redirect:/applications";
    }

    /*
     * Deletes the application with the specified id
     */
    @RequestMapping(value = "/deleteApplication/{id}", method = RequestMethod.GET)
    public String removeApplication(@PathVariable("id") int id) {
        applicationService.deleteApplication(id);
        return "redirect:/applications";
    }

    /*
     * Adds a version to an application
     */
    @RequestMapping(value = "/addApplicationVersion", method = RequestMethod.POST)
    public String addApplicationVersion(@RequestParam("id") int id, @RequestParam("version") String version) {
        applicationVersionService.addApplicationVersion(id, version);
        return "redirect:/applications";
    }

    /*
     * Returns a json list of the complete Library Hierarchy
     */
    @RequestMapping(value = "/libraryHierarchy", method = RequestMethod.GET)
    public String getLibraryHierarchy(Map<String, Object> map) {
        map.put("libraryVendors", libraryVersionService.getLibraryHierarchy());
        return "libraryHierarchy";
    }

    /*
     * Lists the data in the specified application version
     */
    @RequestMapping(value = "/applicationVersion/{id}", method = RequestMethod.GET)
    public String listApplicationVersion(ModelMap modelMap, Map<String, Object> map, @PathVariable("id") int id) {
        ApplicationVersion version = applicationVersionService.getApplicationVersion(id);
        modelMap.addAttribute("id", id);
        map.put("applicationVersion", version);
        map.put("dependencies", libraryVersionService.getDependencies(version));
        map.put("libraryVendors", libraryVersionService.getLibraryHierarchy());
        return "applicationVersionPage";
    }

    /*
     * Adds a ApplicationDependency between the specified ApplicationVersion and LibraryVersion
     */
    @RequestMapping(value = "/addDependency", method = RequestMethod.POST)
    public String addDependency(@RequestParam("appversionid") int appversionid,
                                @RequestParam("versionid") int versionid) {
        libraryVersionService.addDependency(appversionid, versionid);
        return "redirect:/applicationVersion/" + appversionid;
    }

    /*
     * Deletes the dependency with the specified ApplicationVersion ID and LibraryVersion ID
     */
    @RequestMapping(value = "/deleteDependency", method = RequestMethod.GET)
    public String deleteDependency(@RequestParam("appversionid") int appversionid,
                                   @RequestParam("versionid") int versionid) {
        libraryVersionService.deleteDependency(appversionid, versionid);
        return "redirect:/applicationVersion/" + appversionid;
    }


















    @RequestMapping(value = "/cloneApplication/{applicationversionid}", method = RequestMethod.GET)
    public String cloneProduct(ModelMap modelMap, @PathVariable("applicationversionid") int applicationversionid) {
        modelMap.addAttribute("applicationversionid", applicationversionid);
        applicationVersionService.cloneApplication(applicationversionid);
        return "redirect:/applications";
    }

	/*
	 * ------ Applications and Version end ------
	 */

	/*
	 * ------ Library start ------
	 */

    @RequestMapping(value = "/library/{applicationversionid}", method = RequestMethod.GET)
    public String library(ModelMap modelMap, Map<String, Object> map,
                          @PathVariable("applicationversionid") int applicationversionid) {
        modelMap.addAttribute("applicationversionid", applicationversionid);
        map.put("applicationDependencies", new ApplicationDependency());
        map.put("appDep", libraryVersionService.listLibraryVersion(applicationversionid));
        return "library";
    }

    @RequestMapping(value = "/addlibrary/{applicationversionid}", method = RequestMethod.GET)
    public String addLibrary(ModelMap modelMap,
                             @PathVariable("applicationversionid") int applicationversionid) {
        modelMap.addAttribute("applicationversionid", applicationversionid);
        return "addlibrary";
    }

    @RequestMapping(value = "/addlibrary/{applicationversionid}", method = RequestMethod.POST)
    public String addingLibrary(ModelMap modelMap,
                                @PathVariable("applicationversionid") int applicationversionid,
                                @RequestParam("libraryname") String libraryname,
                                @RequestParam("libraryversion") String libraryversion,
                                @RequestParam("vendor") String vendor,
                                @RequestParam("license") String license,
                                @RequestParam("Licensefile") MultipartFile file,
                                @RequestParam("language") String language,
                                @RequestParam("secuniaID") int secuniaID) {

        libraryVersionService.addLibraryVersion(applicationversionid, libraryname,
                libraryversion, vendor, license, file, language, secuniaID);

        return "redirect:/library/" + applicationversionid;
    }

    @RequestMapping(value = "/updatelibrary/{applicationversionid}/{vendorid}/{licenseid}/{libraryid}/{libraryversionid}/{libraryname}/{libraryversion}/{vendor}/{license}/{language}/{secuniaID}", method = RequestMethod.GET)
    public String updateLibrary(ModelMap modelMap,
                                @PathVariable("vendorid") int vendorid,
                                @PathVariable("licenseid") int licenseid,
                                @PathVariable("libraryid") int libraryid,
                                @PathVariable("applicationversionid") int applicationversionid,
                                @PathVariable("libraryversionid") int libraryversionid,

                                @PathVariable("libraryname") String libraryname,
                                @PathVariable("libraryversion") String libraryversion,
                                @PathVariable("vendor") String vendor,
                                @PathVariable("license") String license,
                                @PathVariable("language") String language,
                                @PathVariable("secuniaID") int secuniaID) {

        modelMap.addAttribute("vendorid", vendorid);
        modelMap.addAttribute("licenseid", licenseid);
        modelMap.addAttribute("libraryid", libraryid);
        modelMap.addAttribute("applicationversionid", applicationversionid);
        modelMap.addAttribute("libraryversionid", libraryversionid);

        modelMap.addAttribute("libraryname", libraryname);
        modelMap.addAttribute("libraryversion", libraryversion);
        modelMap.addAttribute("vendor", vendor);
        modelMap.addAttribute("license", license);
        modelMap.addAttribute("language", language);
        modelMap.addAttribute("secuniaID", secuniaID);

        return "updatelibrary";
    }

    @RequestMapping(value = "/updatelibrary/{applicationversionid}/{vendorid}/{licenseid}/{libraryid}/{libraryversionid}", method = RequestMethod.POST)
    public String updatingLibrary(ModelMap modelMap,
                                  @PathVariable("vendorid") int vendorid,
                                  @PathVariable("licenseid") int licenseid,
                                  @PathVariable("libraryid") int libraryid,
                                  @PathVariable("applicationversionid") int applicationversionid,
                                  @PathVariable("libraryversionid") int libraryversionid,

                                  @RequestParam("Licensefile") MultipartFile file,
                                  @RequestParam("libraryname") String libraryname,
                                  @RequestParam("libraryversion") String libraryversion,
                                  @RequestParam("vendor") String vendor,
                                  @RequestParam("license") String license,
                                  @RequestParam("language") String language,
                                  @RequestParam("secuniaID") int secuniaID) {

        libraryVersionService.updateLibrary(vendorid, licenseid, libraryid,
                libraryversionid, libraryname, libraryversion, vendor, license, file,
                language, secuniaID);

        return "redirect:/library/" + applicationversionid;
    }

    @RequestMapping(value = "/removelibrary/{applicationversionid}/{id}", method = RequestMethod.GET)
    public String removeLibrary(ModelMap modelMap,
                                @PathVariable("id") Integer id,
                                @PathVariable("applicationversionid") Integer applicationversionid) {
        // SecurityUtils.getSubject().logout();

        libraryVersionService.removeLibrary(id);

        return "redirect:/library/" + applicationversionid;
    }


    /*
       Returns a list of all libraries regardless of application association
     */
    @RequestMapping(value = "/libraries", method = RequestMethod.GET)
    public String allLibrary(Map<String, Object> map) {
        map.put("LibraryVersion", new LibraryVersion());
        map.put("libList", libraryVersionService.allLibrary());
        return "librariesPage";
    }

    /*
      Adds a library regardless of application association
    */
    @RequestMapping(value = "/addlibraries", method = RequestMethod.POST)
    public String addLibraries(ModelMap modelMap,
                                @RequestParam("libraryname") String libraryname,
                                @RequestParam("libraryversion") String libraryversion,
                                @RequestParam("vendor") String vendor,
                                @RequestParam("license") String license,
                                @RequestParam("Licensefile") MultipartFile file,
                                @RequestParam("language") String language,
                                @RequestParam("secuniaID") int secuniaID) {

        libraryVersionService.addLibraries(libraryname,libraryversion, vendor, license, file, language, secuniaID);

        return "redirect:/libraries" ;
    }
	

	/*
	 * ------ Library end ------
	 */

	/*
	 * ------ License start ------
	 */

    @RequestMapping(value = "/librarylicense/{applicationversionid}/{licenseid}", method = RequestMethod.GET)
    public String listLicense(Map<String, Object> map,
                              @PathVariable("licenseid") Integer id,
                              @PathVariable("applicationversionid") Integer applicationversionid) {
        // SecurityUtils.getSubject().logout();

        map.put("license", new License());
        map.put("licenseList", libraryVersionService.listLicense(id));
        map.put("applicationversionid", applicationversionid);

        return "license";
    }

    @RequestMapping(value = "/downloadlicense/{applicationversionid}/{license}", method = RequestMethod.GET)
    public void downloadLicense(Map<String, Object> map,
                                HttpServletResponse response,
                                @PathVariable("license") Integer licenseid,
                                @PathVariable("applicationversionid") Integer applicationversionid) {
        // SecurityUtils.getSubject().logout();

        List<License> licenses = libraryVersionService.listLicense(licenseid);
        License newLicense = licenses.get(0);


        try {
            System.out.println("crashed after tis");
            response.setHeader("Content-Disposition", "inline;filename=\""
                    + newLicense.getFilename() + "\"");
            System.out.println("file name" + newLicense.getFilename());
            OutputStream out = response.getOutputStream();
            response.setContentType(newLicense.getContenttype());
            IOUtils.copy(newLicense.getText().getBinaryStream(), out);
            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /*
        The about page
     */
    @RequestMapping(value = "/about", method = RequestMethod.GET)
    public String about() {
        return "aboutPage";
    }


	/*
	 * ------ License start ------
	 */

}
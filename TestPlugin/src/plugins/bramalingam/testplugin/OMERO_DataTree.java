/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2014 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package plugins.bramalingam.testplugin;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import omero.ServerError;
import omero.client;
import omero.api.ServiceFactoryPrx;
import omero.model.IObject;
import omero.model.ProjectI;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarText;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarObject;
import plugins.adufour.vars.util.VarListener;
import pojos.DatasetData;
import pojos.ProjectData;

/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class OMERO_DataTree extends EzPlug implements EzStoppable,Block, VarListener<Object>, EzVarListener<String>{
    VarObject                                           varClient;
    EzVarText                                           varProject;
    VarMutable                                          test;
    client                                              client;
    ServiceFactoryPrx                                   session;
    ProjectData                                         project;
    String[]                                            projectnames;


    @Override
    public void declareInput(VarList inputMap) {
        // TODO Auto-generated method stub

        initialize();
        inputMap.add("Client",varClient);
        inputMap.add("Projects",varProject.getVariable());
    }

    @Override
    public void declareOutput(VarList outputMap) {
        // TODO Auto-generated method stub
    }

    @Override
    public void clean() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void execute() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void initialize() {
        // TODO Auto-generated method stub
        varClient = new VarObject("Client",client);
        varClient.addListener(this);
        varProject = new EzVarText("Project", null, 0, false);
        varProject.addVarChangeListener(this);
    }

    @Override
    public void valueChanged(Var<Object> source, Object oldValue,
            Object newValue) {
        // TODO Auto-generated method stub
        client = (omero.client) newValue;
        //        if (client !=null){
        //            session = client.getSession();
        //            try {
        //                List<IObject> projects = testOmero.getProjects(session);
        //                Iterator<IObject> i = projects.iterator();
        //                System.out.println(projects.size());
        //                Set<DatasetData> datasets;
        //                Iterator<DatasetData> j;
        //                DatasetData dataset;
        //                projectnames = new String[projects.size()];
        //                int cntr=0;
        //                while (i.hasNext()) {
        //                    project = new ProjectData((ProjectI) i.next());
        //                    projectnames[cntr]=project.getName().trim();
        //                    datasets = project.getDatasets();
        //                    j = datasets.iterator();
        //                    while (j.hasNext()) {
        //                        dataset = j.next();
        //                        //Do something here
        //                        //If images loaded.
        //                        //dataset.getImages();
        //                    }
        //                    cntr++;
        //                }
        //            } catch (ServerError e) {
        //                // TODO Auto-generated catch block
        //                e.printStackTrace();
        //            }
        //            varProject.setVisible(true);
        //            varProject.setDefaultValues(projectnames, 0, false);
        //        }
        if (client != null){
            varProject.setDefaultValues(new String[]{"Yes", "NO", "MayBe"},0,true);
        }

    }

    @Override
    public void referenceChanged(Var<Object> source,
            Var<? extends Object> oldReference,
            Var<? extends Object> newReference) {
        // TODO Auto-generated method stub

    }

    @Override
    public void variableChanged(EzVar<String> source, String newValue) {
        // TODO Change Dataset list based on selected Project

    }


}

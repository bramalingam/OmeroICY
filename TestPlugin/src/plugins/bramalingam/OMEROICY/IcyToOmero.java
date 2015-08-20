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
package plugins.bramalingam.OMEROICY;

import icy.file.Saver;
import icy.gui.viewer.Viewer;
import icy.imagej.ImageJUtil;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import ij.plugin.frame.RoiManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import omero.ServerError;
import omero.client;
import omero.api.ServiceFactoryPrx;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.ezplug.EzVarText;
import plugins.adufour.vars.lang.VarROIArray;

/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class IcyToOmero extends EzPlug implements EzStoppable,Block{

    public EzVarSequence uxSequenceSelector = new EzVarSequence("Sequence");
    EzButton                                            importButton;
    EzVarText                                           varServer;
    EzVarText                                           varPort;
    EzVarText                                           varUserName;
    EzVarText                                           varPassword;
    EzVarText                                           varDatasetID;
    EzVarText                                           varImportType;
    EzVarText                                           varImageID;
    VarROIArray                                         outROI;

    // some other data
    boolean                                             stopFlag;

    @Override
    public void declareInput(VarList inputMap) {
        initialize();
        inputMap.add("Sequence",uxSequenceSelector.getVariable());
        inputMap.add("ROI",outROI);
        inputMap.add("Server",varServer.getVariable());
        inputMap.add("Port",varPort.getVariable());
        inputMap.add("UserName",varUserName.getVariable());
        inputMap.add("Password",varPassword.getVariable());
        inputMap.add("DatasetID",varDatasetID.getVariable());
        inputMap.add("ImageID",varImageID.getVariable());
        inputMap.add("Import Type",varImportType.getVariable());
    }

    @Override
    public void declareOutput(VarList outputMap) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clean() {
        // TODO Auto-generated method stub
        Thread.yield();
    }

    @Override
    protected void execute() {
        Sequence seq = uxSequenceSelector.getValue();
        String filename = seq.getFilename();
        String[] path = null;
        String outputPath = null;
        if (filename != null){
            path = new String[]{filename};
        }else{
            Viewer viewer = getActiveViewer();
            filename = viewer.getTitle();
            outputPath = System.getProperty("java.io.tmpdir") + "ICY_Processed_" + filename;
            Saver.save(seq, new File(outputPath));
            path = new String[]{outputPath};
        }

        client client = testOmero.omeroLogin(varServer.getValue(), Integer.valueOf(varPort.getValue()), varUserName.getValue(), varPassword.getValue());
        ServiceFactoryPrx session = client.getSession();
        if (varImportType.getValue().equalsIgnoreCase("Image")){
            testOmero.uploadImage(varServer.getValue(), Integer.valueOf(varPort.getValue()), varUserName.getValue(), varPassword.getValue(),Long.valueOf(varDatasetID.getValue()), path);
        }
        else if (varImportType.getValue().equalsIgnoreCase("Attachment")){
            try {
                testOmero.setFileAnnotation(path[0],Long.valueOf(varImageID.getValue()),session,"ICY_Processed_Image");
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ServerError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else if (varImportType.getValue().equalsIgnoreCase("ROI")){
            ROI[] roilist = outROI.getValue();
            RoiManager manager = RoiManager.getInstance();
            System.out.println(roilist.length);

            List<ROI> listroi = new ArrayList<ROI>();
            for(int i=0;i<roilist.length ; i++){
                listroi.add(roilist[i]);
            }

            List<ROI2D> icyRois = ROI2D.getROI2DList(listroi);
            for (int i=0; i<icyRois.size(); i++){
                System.out.println("Iteration:" + i);
                manager.addRoi(ImageJUtil.convertToImageJRoi(icyRois.get(i)));
            }
            path[0] = path[0].substring(0, path[0].indexOf(".")) + ".zip";
            System.out.println(path[0]);

            if (icyRois.size()>0){
                manager.runCommand("Save", path[0]);
                try {
                    testOmero.setFileAnnotation(path[0],Long.valueOf(varImageID.getValue()),session,"ICY_Processed_ROI");
                } catch (NumberFormatException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ServerError e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        if (outputPath != null){
            try {
                Files.deleteIfExists(FileSystems.getDefault().getPath(outputPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        stopFlag = false;
        if(!isHeadLess()){
            super.getUI().setProgressBarMessage("Waiting...");
            int cpt = 0;
            while (!stopFlag)
            {
                cpt++;
                if (cpt % 10 == 0) super.getUI().setProgressBarValue((cpt % 5000000) / 5000000.0);
                Thread.yield();
            }
        }
    }

    @Override
    public void stopExecution()
    {
        // this method is from the EzStoppable interface
        // if this interface is implemented, a "stop" button is displayed
        // and this method is called when the user hits the "stop" button
        stopFlag = true;
    }

    @Override
    protected void initialize() {
        addEzComponent(uxSequenceSelector);
        varServer = new EzVarText("Server Address:", new String[] { "Server Address" }, 0, true);
        varPort = new EzVarText("Port:", new String[]{"port"}, 0, true);
        varUserName = new EzVarText("UserName:", new String[]{"username"}, 0, true);
        varPassword = new EzVarText("Password:", new String[]{"password"}, 0, true);
        varDatasetID = new EzVarText("DatasetID:", new String[]{"Omero DatasetID"},0, true);
        varImportType = new EzVarText("ImportType", new String[]{"Image","Attachment","ROI"},0,true);
        varImageID = new EzVarText("ImageId:", new String[]{"ImageID"}, 0, true);
        outROI        = new VarROIArray("ROI");

        EzGroup groupCredentials = new EzGroup("OMERO Login Credentials", varServer, varPort, varUserName, varPassword,varDatasetID,varImportType);
        addEzComponent(groupCredentials);
        ActionListener importListener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event) {
                // compute a separate sequence to illustrate the color code
                execute();
            }
        };
        importButton = new EzButton("Import To OMERO", importListener);
        addEzComponent(importButton);
    }

}

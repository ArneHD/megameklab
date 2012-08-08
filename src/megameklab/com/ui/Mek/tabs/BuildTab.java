/*
 * MegaMekLab - Copyright (C) 2008
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free  software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package megameklab.com.ui.Mek.tabs;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Mounted;
import megameklab.com.ui.Mek.views.BuildView;
import megameklab.com.ui.Mek.views.CriticalView;
import megameklab.com.util.CriticalTableModel;
import megameklab.com.util.ITab;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.SpringLayoutHelper;
import megameklab.com.util.UnitUtil;

public class BuildTab extends ITab implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = -6756011847500605874L;

    private RefreshListener refresh = null;
    private CriticalView critView = null;
    private CriticalTableModel critList;
    private BuildView buildView = null;
    private JPanel buttonPanel = new JPanel();
    private JPanel mainPanel = new JPanel();

    private JButton autoFillButton = new JButton("Auto Fill");
    private JButton resetButton = new JButton("Reset");
    private JButton compactButton = new JButton("Compact");

    private String AUTOFILLCOMMAND = "autofillbuttoncommand";
    private String RESETCOMMAND = "resetbuttoncommand";
    private String COMPACTCOMMAND = "compactbuttoncommand";

    public BuildTab(Mech unit, EquipmentTab equipment, WeaponTab weapons) {
        this.unit = unit;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        critView = new CriticalView(getMech(), true, refresh);
        buildView = new BuildView(getMech());

        mainPanel.add(buildView);

        autoFillButton.setMnemonic('A');
        autoFillButton.setActionCommand(AUTOFILLCOMMAND);
        resetButton.setMnemonic('R');
        resetButton.setActionCommand(RESETCOMMAND);
        compactButton.setMnemonic('C');
        compactButton.setActionCommand(COMPACTCOMMAND);
        buttonPanel.add(autoFillButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(compactButton);

        mainPanel.add(buttonPanel);

        this.add(critView);
        this.add(mainPanel);
        refresh();
    }

    public JPanel availableCritsPanel() {
        JPanel masterPanel = new JPanel(new SpringLayout());
        Dimension maxSize = new Dimension();

        masterPanel.add(buildView);

        SpringLayoutHelper.setupSpringGrid(masterPanel, 1);
        maxSize.setSize(300, 5);
        masterPanel.setPreferredSize(maxSize);
        masterPanel.setMinimumSize(maxSize);
        masterPanel.setMaximumSize(maxSize);
        return masterPanel;
    }

    public void refresh() {
        removeAllActionListeners();
        critView.updateUnit(unit);
        buildView.updateUnit(unit);
        critView.refresh();
        buildView.refresh();
        addAllActionListeners();
    }

    public JLabel createLabel(String text, Dimension maxSize) {

        JLabel label = new JLabel(text, SwingConstants.TRAILING);

        label.setMaximumSize(maxSize);
        label.setMinimumSize(maxSize);
        label.setPreferredSize(maxSize);

        return label;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(AUTOFILLCOMMAND)) {
            autoFillCrits();
        } else if (e.getActionCommand().equals(RESETCOMMAND)) {
            resetCrits();
        } else if (e.getActionCommand().equals(COMPACTCOMMAND)) {
            compactCrits();
        }
    }

    private void autoFillCrits() {

        for (Mounted mount : buildView.getTableModel().getCrits()) {
            int externalEngineHS = UnitUtil.getBaseChassisHeatSinks(getMech(), getMech().hasCompactHeatSinks());
            for (int location = Mech.LOC_HEAD; location < unit.locations(); location++) {

                if (!UnitUtil.isValidLocation(unit, mount.getType(), location)) {
                    continue;
                }

                int continuousNumberOfCrits = UnitUtil.getHighestContinuousNumberOfCrits(unit, location);
                int critsUsed = UnitUtil.getCritsUsed(unit, mount.getType());
                if (continuousNumberOfCrits < critsUsed) {
                    continue;
                }
                if ((mount.getLocation() == Entity.LOC_NONE)) {
                    if (UnitUtil.isHeatSink(mount) && (externalEngineHS-- > 0)) {
                        continue;
                    }
                }

                try {
                    if (mount.getType().isSpreadable() || (mount.isSplitable() && (critsUsed > 1))) {
                        for (int count = 0; count < critsUsed; count++) {
                            getMech().addEquipment(mount, location, false);
                        }
                    } else {
                        getMech().addEquipment(mount, location, false);
                    }
                    UnitUtil.changeMountStatus(unit, mount, location, Entity.LOC_NONE, false);
                    break;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }
        refresh.refreshAll();

    }

    private void resetCrits() {
        for (Mounted mount : unit.getEquipment()) {
            UnitUtil.removeCriticals(getMech(), mount);
        }
        for (Mounted mount : unit.getEquipment()) {
            UnitUtil.changeMountStatus(unit, mount, Entity.LOC_NONE, Entity.LOC_NONE, false);
        }
        refresh.refreshAll();
    }

    private void compactCrits() {
        UnitUtil.compactCriticals(getMech());
        UnitUtil.reIndexCrits(getMech());
        refresh.refreshAll();
    }

    public void removeAllActionListeners() {
        autoFillButton.removeActionListener(this);
        resetButton.removeActionListener(this);
        compactButton.removeActionListener(this);
    }

    public void addAllActionListeners() {
        autoFillButton.addActionListener(this);
        resetButton.addActionListener(this);
        compactButton.addActionListener(this);
    }

    public void addRefreshedListener(RefreshListener l) {
        refresh = l;
        critView.updateRefresh(refresh);

    }

    public void addCrit(Mounted mount) {
        critList.addCrit(mount);
    }

    public void refreshAll() {
        if (refresh != null) {
            refresh.refreshAll();
        }
    }

}
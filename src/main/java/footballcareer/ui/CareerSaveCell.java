package footballcareer.ui;

import footballcareer.model.Career;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class CareerSaveCell extends ListCell<Career> {
    @Override protected void updateItem(Career career, boolean empty) {
        super.updateItem(career, empty);
        if (empty || career == null) { setGraphic(null); setText(null); return; }
        Label manager = new Label(career.getManagerName()); manager.getStyleClass().add("save-manager");
        Label detail = new Label(career.getControlledTeam().getName() + "  ·  "
                + career.getCurrentSeason().getName() + "  ·  " + career.getCurrentDate());
        detail.getStyleClass().add("club-picker-detail");
        HBox row = new HBox(14, TeamCrestView.create(career.getControlledTeam(), 56),
                new VBox(4, manager, detail));
        row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("save-row");
        setGraphic(row); setText(null);
    }
}

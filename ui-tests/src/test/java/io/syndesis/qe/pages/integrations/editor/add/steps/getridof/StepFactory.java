package io.syndesis.qe.pages.integrations.editor.add.steps.getridof;

import io.syndesis.qe.pages.integrations.editor.add.steps.AdvancedFilter;
import io.syndesis.qe.pages.integrations.editor.add.steps.BasicFilter;
import io.syndesis.qe.pages.integrations.editor.add.steps.Log;
import io.syndesis.qe.pages.integrations.editor.add.steps.Template;

public class StepFactory {

    public static AbstractStep getStep(String stepType, String parameter) {
        if (stepType == null) {
            return null;
        }
        if (stepType.toUpperCase().equals(AbstractStep.StepType.LOG)) {
            return new Log(parameter);
        } else if (stepType.toUpperCase().equals(AbstractStep.StepType.BASIC_FILTER)) {
            return new BasicFilter(parameter);
        } else if (stepType.toUpperCase().equals(AbstractStep.StepType.ADVANCED_FILTER)) {
            return new AdvancedFilter(parameter);
        } else if (stepType.toUpperCase().equals(AbstractStep.StepType.TEMPLATE)) {
            return new Template(parameter);
        }

        return null;
    }
}

package org.sudhir512kj.googledocs.ot;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class OperationalTransform {
    
    public Operation transform(Operation op1, Operation op2) {
        if (op1.getType() == Operation.OperationType.INSERT && 
            op2.getType() == Operation.OperationType.INSERT) {
            return transformInsertInsert(op1, op2);
        } else if (op1.getType() == Operation.OperationType.INSERT && 
                   op2.getType() == Operation.OperationType.DELETE) {
            return transformInsertDelete(op1, op2);
        } else if (op1.getType() == Operation.OperationType.DELETE && 
                   op2.getType() == Operation.OperationType.INSERT) {
            return transformDeleteInsert(op1, op2);
        } else if (op1.getType() == Operation.OperationType.DELETE && 
                   op2.getType() == Operation.OperationType.DELETE) {
            return transformDeleteDelete(op1, op2);
        }
        return op1;
    }
    
    private Operation transformInsertInsert(Operation op1, Operation op2) {
        if (op1.getPosition() < op2.getPosition()) {
            return op1;
        } else if (op1.getPosition() > op2.getPosition()) {
            return Operation.builder()
                .type(op1.getType())
                .position(op1.getPosition() + op2.getText().length())
                .text(op1.getText())
                .userId(op1.getUserId())
                .timestamp(op1.getTimestamp())
                .version(op1.getVersion())
                .build();
        } else {
            return op1.getTimestamp() < op2.getTimestamp() ? op1 : 
                Operation.builder()
                    .type(op1.getType())
                    .position(op1.getPosition() + op2.getText().length())
                    .text(op1.getText())
                    .userId(op1.getUserId())
                    .timestamp(op1.getTimestamp())
                    .version(op1.getVersion())
                    .build();
        }
    }
    
    private Operation transformInsertDelete(Operation op1, Operation op2) {
        if (op1.getPosition() <= op2.getPosition()) {
            return op1;
        } else if (op1.getPosition() >= op2.getPosition() + op2.getLength()) {
            return Operation.builder()
                .type(op1.getType())
                .position(op1.getPosition() - op2.getLength())
                .text(op1.getText())
                .userId(op1.getUserId())
                .timestamp(op1.getTimestamp())
                .version(op1.getVersion())
                .build();
        } else {
            return Operation.builder()
                .type(op1.getType())
                .position(op2.getPosition())
                .text(op1.getText())
                .userId(op1.getUserId())
                .timestamp(op1.getTimestamp())
                .version(op1.getVersion())
                .build();
        }
    }
    
    private Operation transformDeleteInsert(Operation op1, Operation op2) {
        if (op1.getPosition() >= op2.getPosition() + op2.getText().length()) {
            return Operation.builder()
                .type(op1.getType())
                .position(op1.getPosition() + op2.getText().length())
                .length(op1.getLength())
                .userId(op1.getUserId())
                .timestamp(op1.getTimestamp())
                .version(op1.getVersion())
                .build();
        } else if (op1.getPosition() + op1.getLength() <= op2.getPosition()) {
            return op1;
        } else {
            return Operation.builder()
                .type(op1.getType())
                .position(op1.getPosition())
                .length(op1.getLength())
                .userId(op1.getUserId())
                .timestamp(op1.getTimestamp())
                .version(op1.getVersion())
                .build();
        }
    }
    
    private Operation transformDeleteDelete(Operation op1, Operation op2) {
        if (op1.getPosition() >= op2.getPosition() + op2.getLength()) {
            return Operation.builder()
                .type(op1.getType())
                .position(op1.getPosition() - op2.getLength())
                .length(op1.getLength())
                .userId(op1.getUserId())
                .timestamp(op1.getTimestamp())
                .version(op1.getVersion())
                .build();
        } else if (op1.getPosition() + op1.getLength() <= op2.getPosition()) {
            return op1;
        } else {
            int newPos = Math.min(op1.getPosition(), op2.getPosition());
            int newLen = op1.getLength() - Math.min(op1.getLength(), 
                Math.max(0, op2.getPosition() + op2.getLength() - op1.getPosition()));
            return Operation.builder()
                .type(op1.getType())
                .position(newPos)
                .length(Math.max(0, newLen))
                .userId(op1.getUserId())
                .timestamp(op1.getTimestamp())
                .version(op1.getVersion())
                .build();
        }
    }
    
    public String applyOperation(String content, Operation operation) {
        if (operation.getType() == Operation.OperationType.INSERT) {
            return content.substring(0, operation.getPosition()) + 
                   operation.getText() + 
                   content.substring(operation.getPosition());
        } else if (operation.getType() == Operation.OperationType.DELETE) {
            return content.substring(0, operation.getPosition()) + 
                   content.substring(operation.getPosition() + operation.getLength());
        }
        return content;
    }
}

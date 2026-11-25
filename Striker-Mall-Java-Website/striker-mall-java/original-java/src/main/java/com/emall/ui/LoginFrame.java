package com.emall.ui;

import com.emall.dao.UserDao;
import com.emall.model.User;
import com.emall.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class LoginFrame extends JFrame {
    private final UserDao users = new UserDao();

    public LoginFrame() {
        super("K.M.S. E-Mall - Welcome");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.BG);
        JLabel banner = Ui.title("Welcome to K.M.S. E-Mall");
        banner.setHorizontalAlignment(SwingConstants.CENTER);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 10, 8, 10));
        root.add(banner, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sign in", signIn());
        tabs.addTab("Sign up", signUp(tabs));
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        setSize(440, 430);
        setLocationRelativeTo(null);
    }

    private JPanel form(String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(18, 24, 12, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4); g.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; g.weightx = 0; p.add(new JLabel(labels[i]), g);
            g.gridx = 1; g.weightx = 1; p.add(fields[i], g);
        }
        return p;
    }

    private JPanel signIn() {
        JTextField user = new JTextField(); JPasswordField pass = new JPasswordField();
        JPanel p = form(new String[]{"User ID", "Password"}, new JComponent[]{user, pass});
        JButton go = Ui.primary("Sign in");
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2; g.insets = new Insets(14, 4, 4, 4); g.fill = GridBagConstraints.HORIZONTAL;
        p.add(go, g);
        Runnable doLogin = () -> Ui.guard(this, () -> {
            Optional<User> u = users.authenticate(user.getText().trim(), new String(pass.getPassword()));
            if (u.isEmpty()) { Ui.info(this, "Incorrect user ID or password."); pass.setText(""); return; }
            User me = u.get();
            dispose();
            if (me.isAdmin()) new AdminFrame(me).setVisible(true);
            else new CustomerFrame(me).setVisible(true);
        });
        go.addActionListener(e -> doLogin.run());
        pass.addActionListener(e -> doLogin.run());
        return p;
    }

    private JPanel signUp(JTabbedPane tabs) {
        JTextField name = new JTextField(), phone = new JTextField(), uid = new JTextField();
        JPasswordField pw = new JPasswordField(), pw2 = new JPasswordField();
        JPanel p = form(new String[]{"Full name", "Phone (10 digits)", "User ID (4-20 letters/digits/_)", "Password (min 6)", "Confirm password"},
                        new JComponent[]{name, phone, uid, pw, pw2});
        JButton go = Ui.primary("Create account");
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 6; g.gridwidth = 2; g.insets = new Insets(14, 4, 4, 4); g.fill = GridBagConstraints.HORIZONTAL;
        p.add(go, g);
        go.addActionListener(e -> Ui.guard(this, () -> {
            String a = new String(pw.getPassword()), b = new String(pw2.getPassword());
            if (!Validator.name(name.getText())) throw new AppException("Please enter your name.");
            if (!Validator.phone(phone.getText().trim())) throw new AppException("Phone number must be exactly 10 digits.");
            if (!Validator.username(uid.getText().trim())) throw new AppException("User ID must be 4-20 letters, digits or underscore.");
            if (!Validator.password(a)) throw new AppException("Password must be at least 6 characters.");
            if (!a.equals(b)) throw new AppException("Passwords do not match.");
            User u = users.register(name.getText(), phone.getText().trim(), uid.getText().trim(), a);
            dispose();
            new CustomerFrame(u).setVisible(true);
        }));
        return p;
    }
}

sudo pip install ansible
ansible-playbook -u root -i ./ops/staging.inventory ./ops/production_playbook.yml
